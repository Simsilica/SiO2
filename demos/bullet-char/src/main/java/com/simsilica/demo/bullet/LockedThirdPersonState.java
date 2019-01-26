/*
 * $Id$
 * 
 * Copyright (c) 2018, Simsilica, LLC
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

package com.simsilica.demo.bullet;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

import com.simsilica.es.*;
import com.simsilica.mathd.*;

import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.*;


/**
 *  A third person camera that is rigidly locked behind the player's
 *  head and all movement is relative to that.
 *
 *  @author    Paul Speed
 */
public class LockedThirdPersonState extends BaseAppState 
                                    implements AnalogFunctionListener, StateFunctionListener {


    static Logger log = LoggerFactory.getLogger(LockedThirdPersonState.class);

    private EntityData ed;
    private EntityId playerId;
    private WatchedEntity player;
    private Spatial playerAvatar;

    private InputMapper inputMapper;
    private Camera camera;
    private Vector3f headOffset = new Vector3f(0, 0.6f, 0);
    private float cameraDistance = 5;

    private double turnSpeed = 2.5;  // one half complete revolution in 2.5 seconds

    private double speed = 1;
    
    private double pitch = -0.2;
    private double maxPitch = FastMath.HALF_PI;
    private double minPitch = -FastMath.HALF_PI;

    private double yaw;

    // These are the values that we will send to the driver
    private Vec3d move = new Vec3d();
    private Quatd facing = new Quatd();
    private int flags;
    private double forward;
    private double side;
    
    public LockedThirdPersonState( EntityId playerId ) {
        this.playerId = playerId;        
    }
    
    @Override
    protected void initialize( Application app ) {
        this.ed = getState(GameSystemsState.class).get(EntityData.class);
        this.camera = ((Main)app).getCamera();
        
        if( inputMapper == null ) {
            inputMapper = GuiGlobals.getInstance().getInputMapper();
        }
        
        // Most of the movement functions are treated as analog.        
        inputMapper.addAnalogListener(this,
                                      PlayerMovementFunctions.F_Y_ROTATE,
                                      PlayerMovementFunctions.F_X_ROTATE,
                                      PlayerMovementFunctions.F_MOVE,
                                      PlayerMovementFunctions.F_ELEVATE,
                                      PlayerMovementFunctions.F_STRAFE);

        // Only run mode is treated as a 'state' or a trinary value.
        // (Positive, Off, Negative) and in this case we only care about
        // Positive and Off.  See PlayerMovementFunctions for a description
        // of alternate ways this could have been done.
        inputMapper.addStateListener(this,
                                     PlayerMovementFunctions.F_RUN,
                                     PlayerMovementFunctions.F_JUMP);
    }
    
    @Override
    protected void cleanup( Application app ) {
        inputMapper.removeAnalogListener(this,
                                         PlayerMovementFunctions.F_Y_ROTATE,
                                         PlayerMovementFunctions.F_X_ROTATE,
                                         PlayerMovementFunctions.F_MOVE,
                                         PlayerMovementFunctions.F_ELEVATE,
                                         PlayerMovementFunctions.F_STRAFE);
        inputMapper.removeStateListener(this,
                                        PlayerMovementFunctions.F_RUN,
                                        PlayerMovementFunctions.F_JUMP);
    }
    
    @Override
    protected void onEnable() {    
        player = ed.watchEntity(playerId, Position.class);
        

        inputMapper.activateGroup(PlayerMovementFunctions.G_MOVEMENT);        
    }
    
    @Override
    public void update( float tpf ) {
 
        if( playerAvatar == null ) {
            // We'll actually sync directly to the model so that we
            // don't jitter in relation
            playerAvatar = getState(ModelViewState.class).getModel(playerId);
        }
 
        // In this camera mode, movement is relative to the facing direction
        move.set(side * speed, 0, forward * speed);
        facing.mult(move, move);
        
        player.set(new CharInput(move, facing, (byte)flags));   

        Quaternion camRot = new Quaternion().fromAngles((float)-pitch, (float)yaw, 0); 
        camera.setRotation(camRot);
 
        boolean changed = player.applyChanges();
        if( playerAvatar != null ) {
            Vector3f loc = playerAvatar.getWorldTranslation().clone();
            loc.subtractLocal(camRot.mult(Vector3f.UNIT_Z.mult(cameraDistance)));
            camera.setLocation(loc);            
        } else if( changed ) {
            log.warn("Synching to entity instead of spatial.");        
            // We haven't gotten a player model yet so just sync directly to the
            // entity 
            Position pos = player.get(Position.class);
            if( pos == null ) {
                log.warn("Entity has no position.");
            } else {
                Vector3f loc = pos.getLocation().add(headOffset);
                loc.subtractLocal(camRot.mult(Vector3f.UNIT_Z.mult(cameraDistance)));
                camera.setLocation(loc);
            }
        }
    }
    
    @Override
    protected void onDisable() {
        player.release();
        player = null;
        inputMapper.deactivateGroup(PlayerMovementFunctions.G_MOVEMENT);
    }
    
    /**
     *  Implementation of the StateFunctionListener interface.
     */
    @Override
    public void valueChanged( FunctionId func, InputState value, double tpf ) {
        //System.out.println("valueChanged(" + func + ", " + value + ", " + tpf + ")");
 
        // Change the speed based on the current run mode
        // Another option would have been to use the value
        // directly:
        //    speed = 3 + value.asNumber() * 5
        //...but I felt it was slightly less clear here.   
        boolean b = value == InputState.Positive;
        if( func == PlayerMovementFunctions.F_RUN ) {
            if( b ) {
                speed = 2;
            } else {
                speed = 1;
            }
        } else if( func == PlayerMovementFunctions.F_JUMP ) {
            if( b ) {
                flags = flags | CharInput.JUMP;
            } else {
                flags = flags & (~CharInput.JUMP);
            }
        } 
    }

    /**
     *  Implementation of the AnalogFunctionListener interface.
     */
    @Override
    public void valueActive( FunctionId func, double value, double tpf ) {
        //System.out.println("valueActive(" + func + ", " + value + ", " + tpf + ")");
 
        // Setup rotations and movements speeds based on current
        // axes states.    
        if( func == PlayerMovementFunctions.F_Y_ROTATE ) {
            pitch += -value * tpf * turnSpeed;
            if( pitch < minPitch ) {
                pitch = minPitch;
            }
            if( pitch > maxPitch ) {
                pitch = maxPitch;
            }
        } else if( func == PlayerMovementFunctions.F_X_ROTATE ) {
            yaw += -value * tpf * turnSpeed;
            if( yaw < 0 ) {
                yaw += Math.PI * 2;
            }
            if( yaw > Math.PI * 2 ) {
                yaw -= Math.PI * 2;
            }
            
            // In this camera mode, we only send the yaw to the driver
            facing.fromAngles(0, yaw, 0);
        } else if( func == PlayerMovementFunctions.F_MOVE ) {
            this.forward = value;
        } else if( func == PlayerMovementFunctions.F_STRAFE ) {
            this.side = -value;
        } 
    }
    
}




