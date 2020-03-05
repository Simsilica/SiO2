/*
 * $Id$
 *
 * Copyright (c) 2019, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.input;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.*;

import com.simsilica.mathd.*;

/**
 *  An app state for moving a camera or camera-like object that follows
 *  the "quake style" axis ordering and supports yaw and pitch rotation.
 *  "Quake style" in this case means that yaw is always applied before pitch
 *  in such a way that it is impossible to induce a 'roll' just through
 *  yaw and pitch movements.  The actual target of the movement can be
 *  set by supplying a MovementDriver.
 *
 *  @author    Paul Speed
 */
public class MovementState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(MovementState.class);

    private InputMapper inputMapper;

    private MovementTarget target;
    private MoveInputListener inputListener = new MoveInputListener();

    private double turnSpeed = 1;

    private double yaw = Math.PI;

    private double pitch = 0;
    private double maxPitch = Math.PI * 0.5;
    private double minPitch = -Math.PI * 0.5;

    private Quatd facing = new Quatd().fromAngles(pitch, yaw, 0);
    private Vec3d movementForce = new Vec3d();

    private double walkSpeed = 3;  // units/sec
    private double runSpeed = 10;  // units/sec
    private double currentSpeed = walkSpeed;

    private boolean invertPitch;

    public MovementState() {
    }

    /**
     *  Sets the current movement target.  The default implementation
     *  sets up a CameraMovementTarget during initialization if no other
     *  target has been provided.
     */
    public void setMovementTarget( MovementTarget target ) {
        this.target = target;
        initializeRotation(target.getRotation());
    }

    public MovementTarget getMovementTarget() {
        return target;
    }

    /**
     *  Sets the current pitch, clamped by minPitch and maxPitch.  The
     *  calculated rotation value will be passed directly to the MovementTarget.
     */
    public void setPitch( double pitch ) {
        this.pitch = clampPitch(pitch);
        updateFacing();
    }

    public double getPitch() {
        return pitch;
    }

    public void setMaxPitch( double maxPitch ) {
        this.maxPitch = maxPitch;
    }

    public double getMaxPitch() {
        return maxPitch;
    }

    public void setMinPitch( double minPitch ) {
        this.minPitch = minPitch;
    }

    public double getMinPitch() {
        return minPitch;
    }

    /**
     *  Set to true to invert all pitch control.  This will affect all
     *  "pitch" axes simultanesouly, ie: joystick, mouse, etc. versus
     *  inverting those axes specifically with MovementFunctions.MOUSE_Y_LOOK, etc.
     */
    public void setInvertPitch( boolean invertPitch ) {
        this.invertPitch = invertPitch;
    }

    public boolean isInvertPitch() {
        return invertPitch;
    }

    /**
     *  Sets the movement speed multiplier used when run mode is not engaged.
     *  Defaults to 3 units/second.
     */
    public void setWalkSpeed( double walkSpeed ) {
        this.walkSpeed = walkSpeed;
    }

    public double getWalkSpeed() {
        return walkSpeed;
    }

    /**
     *  Sets the movement speed multiplier used when run mode is engaged.
     *  Defaults to 10 units/second.
     */
    public void setRunSpeed( double runSpeed ) {
        this.runSpeed = runSpeed;
    }

    public double getRunSpeed() {
        return runSpeed;
    }

    /**
     *  Manually turns on/off running mode that controls the speed
     *  multiplier.
     */
    public void setRunning( boolean running ) {
        if( running ) {
            this.currentSpeed = runSpeed;
        } else {
            this.currentSpeed = walkSpeed;
        }
    }

    public boolean isRunning() {
        return runSpeed == currentSpeed;
    }

    /**
     *  Sets the current yaw. The calculated rotation value will be passed
     *  directly to the MovementTarget.
     */
    public void setYaw( double yaw ) {
        this.yaw = yaw;
        updateFacing();
    }

    public double getYaw() {
        return yaw;
    }

    /**
     *  Sets the yaw and pitch at the same time.  The calculated rotation value
     *  will be passed directly to the MovementTarget.
     */
    public void initializeRotation( double yaw, double pitch ) {
        this.pitch = clampPitch(pitch);
        this.yaw = yaw;
        updateFacing();
    }

    /**
     *  Sets the yaw and pitch by extracting them from the specified
     *  quaternion using toAngles().
     *  The calculated rotation value will be passed directly to the MovementTarget.
     */
    public void initializeRotation( Quatd rotation ) {
        if( rotation == null ) {
            throw new IllegalArgumentException("Rotation cannot be null");
        }
        // Do our best
        double[] angle = rotation.toAngles(null);
        this.pitch = clampPitch(angle[0]);
        this.yaw = angle[1];
        log.info("initialized yaw/pitch to:" + yaw + ", " + pitch);
        updateFacing();
    }

    /**
     *  Returns the current facing rotation as a quaternion.
     */
    public Quatd getRotation() {
        return facing;
    }

    /**
     *  Returns the current movement force in all three axes.
     */
    public Vec3d getMovementForce() {
        return movementForce;
    }

    @Override
    public void update( float tpf ) {

        // Just pass the current movement values to the target and let
        // it sort out how it wants to update position
        target.move(facing, movementForce, tpf);
    }

    @Override
    protected void initialize( Application app ) {
        if( inputMapper == null ) {
            inputMapper = GuiGlobals.getInstance().getInputMapper();
        }

        if( target == null ) {
            // Setup one for the application camera
            log.info("Setting up default CameraMovementTarget for the application camera");
            setMovementTarget(new CameraMovementTarget(app.getCamera()));
        }

        // Make sure the default mappings are initialized if not already
        // setup
        MovementFunctions.initializeDefaultMappings(inputMapper);

        // Most of the movement functions are treated as analog.
        inputMapper.addAnalogListener(inputListener,
                                      MovementFunctions.F_Y_LOOK,
                                      MovementFunctions.F_X_LOOK,
                                      MovementFunctions.F_MOVE,
                                      MovementFunctions.F_ELEVATE,
                                      MovementFunctions.F_STRAFE);

        // Only run mode is treated as a 'state' or a trinary value.
        // (Positive, Off, Negative) and in this case we only care about
        // Positive and Off.  See MovementFunctions for a description
        // of alternate ways this could have been done.
        inputMapper.addStateListener(inputListener,
                                     MovementFunctions.F_RUN);
    }

    @Override
    protected void cleanup( Application app ) {
        inputMapper.removeAnalogListener(inputListener,
                                         MovementFunctions.F_Y_LOOK,
                                         MovementFunctions.F_X_LOOK,
                                         MovementFunctions.F_MOVE,
                                         MovementFunctions.F_ELEVATE,
                                         MovementFunctions.F_STRAFE);
        inputMapper.removeStateListener(inputListener,
                                        MovementFunctions.F_RUN);
    }

    @Override
    protected void onEnable() {
        // Make sure our input group is enabled
        inputMapper.activateGroup(MovementFunctions.GROUP_MOVEMENT);

        // And kill the cursor
        GuiGlobals.getInstance().setCursorEventsEnabled(false);

        // A 'bug' in Lemur causes it to miss turning the cursor off if
        // we are enabled before the MouseAppState is initialized.
        getApplication().getInputManager().setCursorVisible(false);

        // Match our concept of rotation to whatever is currently set to the
        // target
        initializeRotation(target.getRotation());
    }

    @Override
    protected void onDisable() {
        inputMapper.deactivateGroup(MovementFunctions.GROUP_MOVEMENT);
        GuiGlobals.getInstance().setCursorEventsEnabled(true);
    }

    protected double clampPitch( double pitch ) {
        if( pitch < minPitch ) {
            return minPitch;
        }
        if( pitch > maxPitch ) {
            return maxPitch;
        }
        return pitch;
    }

    protected void updateFacing() {
        facing.fromAngles(pitch * (invertPitch ? -1 : 1), yaw, 0);
        target.setRotation(facing);
    }

    private class MoveInputListener implements AnalogFunctionListener, StateFunctionListener {

        @Override
        public void valueChanged( FunctionId func, InputState value, double tpf ) {

            // Change the speed based on the current run mode
            // Another option would have been to use the value
            // directly:
            //    speed = 3 + value.asNumber() * 5
            //...but I felt it was slightly less clear here.
            boolean b = value == InputState.Positive;
            if( func == MovementFunctions.F_RUN ) {
                if( b ) {
                    currentSpeed = runSpeed;
                } else {
                    currentSpeed = walkSpeed;
                }
            }
        }

        @Override
        public void valueActive( FunctionId func, double value, double tpf ) {

            // Setup rotations and movements speeds based on current
            // axes states.
            if( func == MovementFunctions.F_Y_LOOK ) {
                pitch = clampPitch(pitch - value * tpf * turnSpeed);
                updateFacing();
            } else if( func == MovementFunctions.F_X_LOOK ) {
                yaw += -value * tpf * turnSpeed;
                if( yaw < 0 ) {
                    yaw += Math.PI * 2;
                }
                if( yaw > Math.PI * 2 ) {
                    yaw -= Math.PI * 2;
                }
                updateFacing();
            } else if( func == MovementFunctions.F_MOVE ) {
                movementForce.z = value * currentSpeed;
            } else if( func == MovementFunctions.F_STRAFE ) {
                // Mappings are set to treat right as positive and left as
                // negative because that's the way things like mice and joysticks
                // are already setup.  However, this is backwards from JME's handedness
                // which when looking down Z, left is positive.  So we'll swap the
                // force.
                movementForce.x = -value * currentSpeed;
            } else if( func == MovementFunctions.F_ELEVATE ) {
                movementForce.y = value * currentSpeed;
            }
        }
    }
}

