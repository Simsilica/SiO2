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
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import com.simsilica.es.EntityId;

/**
 *  A ghost object that is associated directly to an entity.
 *
 *  @author    Paul Speed
 */
public class EntityGhostObject extends PhysicsGhostObject 
                             implements EntityPhysicsObject<PhysicsGhostObject> {
 
    private EntityId id;
    private EntityId parentId;
    private EntityRigidBody parent;
    private SpawnPosition parentOffset;
    private Vector3f vTemp;
    private Quaternion qTemp;    
    
    public EntityGhostObject( EntityId id, CollisionShape shape ) {
        super(shape);
        this.id = id;
    }

    @Override
    public EntityId getId() {
        return id;
    }
    
    @Override
    public PhysicsGhostObject getObject() {
        return this;    
    }
 
    /**
     *  Always returns Mass.TYPE_INTANGIBLE.
     */
    @Override
    public int getMassType() {
        return Mass.TYPE_INTANGIBLE;
    }
  
    /**
     *  Returns the ID of the parent If this ghost is 'attached' to a parent object. 
     */
    public EntityId getParentId() {
        return parentId;
    }
 
    /**
     *  Returns the parent rigid body if this ghost is 'attached' to a parent object.
     */
    public EntityRigidBody getParent() {
        return parent;
    }
 
    /**
     *  Used internally to set the parent rigid body when this ghost is 'attached' to
     *  a parent object.
     */
    protected void setParent( EntityId parentId, EntityRigidBody parent, SpawnPosition parentOffset ) {
        this.parentId = parentId;
        this.parentOffset = parentOffset;
        setParent(parent);
    }

    /**
     *  Used internally to set the parent rigid body when this ghost is 'attached' to
     *  a parent object.
     */
    protected void setParent( EntityRigidBody parent ) {
        this.parent = parent;
        if( parent != null ) {
            vTemp = new Vector3f();
            qTemp = new Quaternion();
        }
    } 

    protected boolean updateToParent() {
        if( parent == null ) {
            return false;
        }
 
        parent.getPhysicsLocation(vTemp);
        parent.getPhysicsRotation(qTemp);
            
        setPhysicsLocation(vTemp.add(qTemp.mult(parentOffset.getLocation())));
        setPhysicsRotation(qTemp.mult(parentOffset.getOrientation()));
 
        return true;           
    }
     
    @Override
    public Vector3f getPhysicsLocation( Vector3f trans ) {
        return super.getPhysicsLocation(trans);
    }

    @Override
    public Quaternion getPhysicsRotation( Quaternion rot ) {
        return super.getPhysicsRotation(rot);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("id", id)
            .toString();
    }     
}
