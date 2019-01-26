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

import com.simsilica.es.*;

/**
 *  Indicates the type of movement that a parent entity is capable of
 *  and is typically used to translate raw object movement into animation.
 *  This is generally always combined with a Parent component that denotes
 *  the entity to which this mobility state applies.  A parent may have
 *  multiple mobility states at a time (for example, walking and aiming)
 *  that are combined and/or simultanesouly affected by the movement or
 *  orientation of the object in some way.
 *
 *  @author    Paul Speed
 */
public class Mobility implements EntityComponent {

    private int id;
    private double baseSpeed;

    protected Mobility() {
    }
    
    public Mobility( int id ) {
        this(id, 1.0);
    }
    
    public Mobility( int id, double baseSpeed ) {
        this.id = id;
        this.baseSpeed = baseSpeed;
    }
    
    public int getMobilityId() {
        return id; 
    }
 
    public String getMobilityName( EntityData ed ) {
        return ed.getStrings().getString(id);
    }
    
    public double getBaseSpeed() {
        return baseSpeed;
    }
    
    public static Mobility create( String name, double baseSpeed, EntityData ed ) {
        return new Mobility(ed.getStrings().getStringId(name, true), baseSpeed);
    }
    
    public boolean equals( Object o ) {
        if( o == this ) {
            return true;
        }
        if( o == null || o.getClass() != getClass() ) {
            return false;
        }
        Mobility other = (Mobility)o;
        return other.id == id && other.baseSpeed == baseSpeed;
    }
 
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    } 
}
