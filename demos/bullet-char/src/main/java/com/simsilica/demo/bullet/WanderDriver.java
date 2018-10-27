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
import com.simsilica.sim.SimTime;

import com.simsilica.bullet.*;

/**
 *  A control driver that will make an object wander around, never
 *  straying too far from 0, 0.
 *
 *  @author    Paul Speed
 */
public class WanderDriver implements ControlDriver {
    
    private Entity entity;
    private EntityRigidBody body;
    
    private Vector3f vTemp = new Vector3f();
    private Quaternion qTemp = new Quaternion();
    private float[] angles = new float[3];

    private float headingAngle;
 
    // Random but predicable   
    private static int nextSeed = 0;
    private Random random = new Random(nextSeed++);

    // Used by the addCollision() method to avoid constantly reacquiring the
    // location and rotation
    private Vector3f location = new Vector3f();
    private Quaternion orientation = new Quaternion();
    private Vector3f dir = new Vector3f();
    private Vector3f left = new Vector3f();
    private boolean needsUpdate = true;
 
    private float minDistanceSq;
    private float minLeft;
    private float minFwd;
    
    public WanderDriver( Entity entity ) {
        this.entity = entity;
    }

    @Override
    public void initialize( EntityRigidBody body ) {
        this.body = body;
    }
 
    @Override
    public void addCollision( EntityPhysicsObject otherBody, PhysicsCollisionEvent event ) {
        //if( otherBody instanceof EntityGhostObject ) {
        //    return;
        //}
 
        Vector3f normal = event.getNormalWorldOnB();
 
        // See if the contact is the floor... we don't care about that.
        float up = Math.abs(Vector3f.UNIT_Y.dot(normal));
        if( up > 0.9 ) {
            return;
        }
        
        //System.out.println("addCollision(" + otherBody + ")");
 
        if( needsUpdate ) {
            location = body.getPhysicsLocation(location);
            orientation = body.getPhysicsRotation(orientation);
            dir = orientation.mult(Vector3f.UNIT_Z);
            left = orientation.mult(Vector3f.UNIT_X);
            needsUpdate = false;
        }
        
        Vector3f wp = event.getPositionWorldOnB();
        Vector3f relative = vTemp.set(wp).subtractLocal(location);
 
        float fwd = dir.dot(relative);
        float x = left.dot(relative);
        
        // Now we know where the contact is in our field of view
        
        if( fwd < 0 ) {
            // It's behind us... don't bother with it
            return;
            
            // Note: could also probably ignore contacts that are facing away from
            // us though there is some A versus B juggling to do with that check
        }
        
        float distSq = fwd * fwd + x * x;
        if( distSq < minDistanceSq ) {
            minDistanceSq = distSq;
            minLeft = x;
            minFwd = fwd;
        }         
    }
    
    protected void invalidateCollisionData() {
        needsUpdate = true;
        minDistanceSq = Float.POSITIVE_INFINITY;
    }      
 
    protected float calculateWanderSteer() {
        // We do this by imagining a wheel in front of us that randomly
        // turns left or right... we will steer towards wherever the 0 degree
        // mark on the wheel happens to be in our field of view.
        float volatility = 0.4f;
        headingAngle += random.nextFloat() * volatility - (volatility * 0.499);
        while( headingAngle > FastMath.TWO_PI ) {
            headingAngle -= FastMath.TWO_PI;
        }
        while( headingAngle < -FastMath.TWO_PI ) {
            headingAngle += FastMath.TWO_PI;
        }
         
        float sin = FastMath.sin(headingAngle);
        float cos = FastMath.cos(headingAngle);
        
        // Project it in front of us some distance
        // The farther out, the less dramatic turning will be
        cos += 10f; 
 
        // The heading is imaginary and already in local space...
        Vector3f heading = new Vector3f(sin, 0, cos).normalizeLocal();
        //Vector3f left = Vector3f.UNIT_X;
        
        //System.out.println("Steer:" + heading.x + "  headingAngle:" + headingAngle + "   heading:" + heading);
        
        return heading.x; //heading.dot(left);        
    }
          
    @Override
    public void update( SimTime time, EntityRigidBody body ) {
        body.getPhysicsRotation(qTemp);
        body.getAngularVelocity(vTemp);
        
        // Kill any non-yaw orientation
        qTemp.toAngles(angles);
        if( angles[0] != 0 || angles[2] != 0 ) {
            angles[0] = 0;
            angles[2] = 0;
            body.setPhysicsRotation(qTemp.fromAngles(angles));
        }
        
        // Kill any non-yaw rotation
        if( vTemp.x != 0 && vTemp.z != 0 ) {
            vTemp.x = 0;
            vTemp.y *= 0.95f; // Let's see if we can dampen the spinning
            vTemp.z = 0;
            body.setAngularVelocity(vTemp);
        }        

        float steer = 0;

        // See if there is an impending obstacle
        if( minDistanceSq < Float.POSITIVE_INFINITY ) {
            //System.out.println("Impending collision, minLeft:" + minLeft);
            
            // Because we are only wandering aimlessly, it's ok to just let the
            // obstacle avoidance override the wander.
            if( Math.abs(minLeft) < 0.01 ) {
                // It's basically right in front of us so we'll pick a random direction
                // ...which note might randomly be just as bad... but might not.
                steer = (float)Math.signum((Math.random() - 0.5) * 2); 
            } else {
                // If the object is far to either side then we only want to turn
                // a little.  If it's close to 0 then we want to turn a lot.
                steer = -(Math.signum(minLeft) - minLeft);
            }
            
            //System.out.println("steer:" + steer + "   dist:" + minDistanceSq);
        } else {    
            // Else steer randomly...
            steer = calculateWanderSteer();
        }                      
                       
        float maxSpeed = 1;               
        float acceleration = 3;
 
        Vector3f velocity = body.getLinearVelocity();
        float speed = velocity.length();
    
        if( speed < maxSpeed ) {
            body.applyImpulse(qTemp.mult(new Vector3f(0, 0, acceleration)), new Vector3f());
        }
        body.applyTorqueImpulse(new Vector3f(0, steer, 0));
        
        // Any new addCollision() calls will definitely need a new reference
        // frame since we are starting a new update frame.
        invalidateCollisionData();
    }
    
    @Override
    public void terminate( EntityRigidBody body ) {
    }
}


