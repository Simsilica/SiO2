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

import com.jme3.bullet.collision.*;
import com.jme3.math.*;

import com.simsilica.es.*;
import com.simsilica.es.common.Decay;

/**
 *  A collision listener implementation that will publish contact entities.
 *
 *  @author    Paul Speed
 */
public class DefaultContactPublisher implements EntityCollisionListener {

    private EntityData ed;
    
    public DefaultContactPublisher( EntityData ed ) {
        this.ed = ed;
    } 

    protected Contact createContact( EntityPhysicsObject object1, EntityPhysicsObject object2, PhysicsCollisionEvent event ) {
        float energy = 0; 
        Vector3f wp = event.getPositionWorldOnB().clone(); 
        Vector3f normal = event.getNormalWorldOnB().clone();
 
        // If object1 is null then we'll swap everything so that we always have a valid
        // object1.  It's just nicer.
        if( object1 == null ) {
            object1 = object2;
            object2 = null;
            wp.set(event.getPositionWorldOnA());
            normal.negateLocal();
        }
         
        // If neither of the bodies are ghosts...       
        if( !(object1 instanceof EntityGhostObject) && !(object2 instanceof EntityGhostObject) ) {
            // Calculate the energy of the collision
            Vector3f v1 = (object1 instanceof EntityRigidBody) ? ((EntityRigidBody)object1).getLastVelocity() : Vector3f.ZERO;         
            Vector3f v2 = (object2 instanceof EntityRigidBody) ? ((EntityRigidBody)object2).getLastVelocity() : Vector3f.ZERO;         
 
            // Normal is always pointing towards object1 and away from object2...  because
            // normal comes from getNormalWorldOnB().
            float dot1 = -normal.dot(v1);
            float dot2 = normal.dot(v2);
            energy = dot1 + dot2;
        }

        return Contact.create(object1, object2, wp, normal, energy);
    }

    @Override
    public void collision( EntityPhysicsObject object1, EntityPhysicsObject object2, PhysicsCollisionEvent event ) {
        //System.out.println("collision:" + object1 + " -> " + object2); 

        Contact c = createContact(object1, object2, event);    
        createEntity(c);
    }
 
    /**
     *  Called by the collision method to create the contact entity and populate its Contact
     *  and Decay components.  Can be overridden by subclasses as needed to have different behavior.
     *  The default behavior sets a Decay component with no time so it will be removed when the
     *  decay system first detects it.
     */   
    protected EntityId createEntity( Contact c ) {
        EntityId contactEntity = ed.createEntity();
        ed.setComponents(contactEntity, c, new Decay());
        return contactEntity;                
    }
}

