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

import java.util.Random;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.math.*;

import com.simsilica.es.*;
import com.simsilica.mathd.*;
import com.simsilica.sim.SimTime;

import com.simsilica.bullet.*;

/**
 *  A control driver that will set the linear velocity and 
 *  position of a kinematic object based on a defined path.
 *
 *  @author    Paul Speed
 */
public class PlatformPathDriver implements ControlDriver {
    
    private Entity entity;
    private EntityRigidBody body;
    

    private PlatformPath path;

    private Vector3f start;
    private Vector3f end;
    private Vector3f position; 
    private double startTime; 
    
    public PlatformPathDriver( Entity entity ) {
        this.entity = entity;
    }

    public void setPath( PlatformPath path ) {
        this.path = path;
        if( path == null ) {
            return;
        }
        this.start = path.getStart().toVector3f();
        this.end = path.getEnd().toVector3f();
        this.position = new Vector3f();
    }
    
    public PlatformPath getPath() {
        return path;
    }

    @Override
    public void initialize( EntityRigidBody body ) {
        this.body = body;
        body.setKinematic(true);
    }
 
    @Override
    public void addCollision( EntityPhysicsObject otherBody, PhysicsCollisionEvent event ) {
    }
    
    @Override
    public void update( SimTime time, EntityRigidBody body ) {
    
        if( path == null ) {
            return;
        }
 
        if( startTime == 0 ) {
            startTime = time.getTimeInSeconds();
        }
 
        // If oscillating then duration is really * 2.
        double t = (time.getTimeInSeconds() - startTime) % (path.getDuration() * 2);
        t = t / path.getDuration();
        if( t > 1 ) {
            t = 1 - (t - 1);
        }
        
        position.interpolateLocal(start, end, (float)t);
        
        //System.out.println("time:" + t + "  pos:" + position);
        
        body.setPhysicsLocation(position);                
    }
    
    @Override
    public void terminate( EntityRigidBody body ) {
    }
}


