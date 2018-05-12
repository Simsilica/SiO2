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

package com.simsilica.bullet;

import com.google.common.base.MoreObjects;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.simsilica.es.EntityId;

/**
 *  A rigid body that is associated directly to an entity.
 *
 *  @author    Paul Speed
 */
public class EntityRigidBody extends PhysicsRigidBody 
                             implements EntityPhysicsObject<PhysicsRigidBody> {
 
    private EntityId id;
    private Mass mass;
    private ControlDriver driver;
    private Vector3f lastVelocity = new Vector3f();
    
    public EntityRigidBody( EntityId id, CollisionShape shape, Mass mass ) {
        super(shape, mass.getMass());
        this.id = id;
        this.mass = mass;
    }

    @Override
    public EntityId getId() {
        return id;
    }
    
    @Override
    public PhysicsRigidBody getObject() {
        return this;    
    }
 
    @Override
    public int getMassType() {
        return mass.getType();
    }
 
    /**
     *  Sets a driver object that will be called once per frame to 'control'
     *  the object.
     */
    public void setControlDriver( ControlDriver driver ) {
        if( this.driver != null ) {
            this.driver.terminate(this);
        }
        this.driver = driver;
        if( this.driver != null ) {
            this.driver.initialize(this);
        }        
    }
    
    @Override
    public ControlDriver getControlDriver() {
        return driver;
    }
 
    @Override
    public Vector3f getPhysicsLocation( Vector3f trans ) {
        return super.getPhysicsLocation(trans);
    }

    @Override
    public Quaternion getPhysicsRotation( Quaternion rot ) {
        return super.getPhysicsRotation(rot);
    }

    /**
     *  Stores the current velocity for later retrieval.
     */
    public void updateLastVelocity() {
        lastVelocity.set(getLinearVelocity());
    }
    
    public Vector3f getLastVelocity() {
        return lastVelocity;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("id", id)
            .toString();
    }     
}
