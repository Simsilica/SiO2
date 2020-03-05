/*
 * $Id$
 *
 * Copyright (c) 2020, Simsilica, LLC
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

import com.jme3.renderer.Camera;
import com.jme3.math.*;

import com.simsilica.mathd.*;

/**
 *  Default MovementTarget implementation that directly manipulates
 *  a JME Camera object.
 *
 *  @author    Paul Speed
 */
public class CameraMovementTarget implements MovementTarget {

    private Camera camera;
    private boolean relativeForward = true;
    private boolean relativeUp = true;

    public CameraMovementTarget( Camera camera ) {
        if( camera == null ) {
            throw new IllegalArgumentException("Camera cannot be null");
        }
        this.camera = camera;
    }

    /**
     *  When set to true, forward/back will be based on the relative up
     *  vector.  When false, forward/back will be clamped into the x/z plane.
     *  Defaults to true so the camera will move forward/back in its own
     *  current frame of reference which makes more sense for free-flight
     *  than walking.
     */
    public void setRelativeForward( boolean relativeForward ) {
        this.relativeForward = relativeForward;
    }

    public boolean isRelativeForward() {
        return relativeForward;
    }

    /**
     *  When set to true, up/down will be based on the relative up
     *  vector.  When false, up/down will be based on the absoluate world-space
     *  up vector.  Defaults to true so the camera will move up/down in its own
     *  current frame of reference which usually makes more sense in free-flight
     *  than an absoluate frame of reference.
     */
    public void setRelativeUp( boolean relativeUp ) {
        this.relativeUp = relativeUp;
    }

    public boolean isRelativeUp() {
        return relativeUp;
    }

    @Override
    public void move( Quatd rotation, Vec3d movementForces, double tpf ) {
        if( movementForces.lengthSq() == 0 ) {
            // No movement
            return;
        }

        Vector3f loc = camera.getLocation();

        if( relativeUp && relativeForward ) {
            // We can simply rotated the movement vector and scale it by tpf
            loc.addLocal(rotation.mult(movementForces).multLocal(tpf).toVector3f());
        } else {
            Vec3d move = new Vec3d();

            // We'll calculate each axis separately

            // Forward
            if( relativeForward ) {
                move.addLocal(rotation.mult(Vec3d.UNIT_Z).multLocal(movementForces.z * tpf));
            } else {
                // Clamp the rotated direction vector to the x/z plane (ie: remove the y component)
                Vec3d dir = rotation.mult(Vec3d.UNIT_Z);
                dir.y = 0;
                dir.normalizeLocal();
                move.addLocal(dir.multLocal(movementForces.z * tpf));
            }

            // Strafe
            move.addLocal(rotation.mult(Vec3d.UNIT_X).multLocal(movementForces.x * tpf));

            // Up/down
            if( relativeUp ) {
                move.addLocal(rotation.mult(Vec3d.UNIT_Y).multLocal(movementForces.y * tpf));
            } else {
                move.addLocal(0, movementForces.y * tpf, 0);
            }
            loc.addLocal(move.toVector3f());
        }
        camera.setLocation(loc);
    }

    @Override
    public void setRotation( Quatd rotation ) {
        camera.setRotation(rotation.toQuaternion());
    }

    @Override
    public Quatd getRotation() {
        return new Quatd(camera.getRotation());
    }
}


