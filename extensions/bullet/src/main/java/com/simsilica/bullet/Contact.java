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

import com.jme3.math.*;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;

/**
 *  Holds the information about a contact between two EntityPhysicsObjects.
 *
 *  @author    Paul Speed
 */
public class Contact implements EntityComponent {

    private EntityId entity1;
    private int type1;
    
    private EntityId entity2;
    private int type2;
 
    private int typeMask;
    
    private Vector3f location;   
    private Vector3f normal;
    private float energy;   

    private Contact() {
    }
 
    public Contact( EntityId entity1, int type1, EntityId entity2, int type2,
                    Vector3f location, Vector3f normal, float energy ) {
        this.entity1 = entity1;
        this.type1 = type1;
        this.entity2 = entity2;
        this.type2 = type2;
        this.typeMask = type1 | type2;
        this.location = location;
        this.normal = normal; 
        this.energy = energy;
    }   
 
    public static Contact create( EntityPhysicsObject object1, EntityPhysicsObject object2,
                                  Vector3f wp, Vector3f normal, float energy ) {
        Contact result = new Contact();
        if( object1 != null ) {
            result.entity1 = object1.getId();
            result.type1 = object1.getMassType();
        }
        if( object2 != null ) {
            result.entity2 = object2.getId();
            result.type2 = object2.getMassType();
        }
        result.typeMask = result.type1 | result.type2;
        result.location = wp;
        result.normal = normal;
        result.energy = energy;
        
        return result;
    } 
 
    public EntityId getEntity1() {
        return entity1;
    }
    
    public int getType1() {
        return type1;
    }
    
    public EntityId getEntity2() {
        return entity2;
    }
    
    public int getType2() {
        return type2;
    }
 
    public int getTypeMask() {
        return typeMask;
    }   
    
    public Vector3f getLocation() {
        return location;
    }

    public Vector3f getNormal() {
        return normal;
    }
    
    public float getEnergy() {
        return energy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                //.omitNullValues()
                .add("entity1", entity1)
                .add("type1", "0b" + Integer.toBinaryString(type1))
                .add("entity2", entity2)
                .add("type2", "0b" + Integer.toBinaryString(type2))
                .add("location", location)
                .add("normal", normal)
                .add("energy", energy)
                .add("typeMask", typeMask)
                .toString(); 
    } 
}
