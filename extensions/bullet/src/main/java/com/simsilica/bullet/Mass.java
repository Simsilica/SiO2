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

import com.simsilica.es.*;

/**
 *  The mass settings of a physical object.
 *
 *  @author    Paul Speed
 */
public class Mass implements EntityComponent {

    /**
     *  Default included type representing an object that may or may not have
     *  mass but is 'intangible' by default.  Ghosts objects do not have a Mass
     *  component but will return this type as their mass type when asked.
     *  It is ultimately up to the game implementation itself to decide how
     *  to use these values. 
     */
    public static final int TYPE_INTANGIBLE = 0;
    
    /**
     *  Default included type representing any solid object (if using bit masking).
     *  All Mass components are created with this type by default.  It is up the
     *  game implementation itself to give meaning to these values in how it
     *  filters its contacts.
     */
    public static final int TYPE_SOLID = 0xffffffff;

    private float mass;
    private int type;

    protected Mass() {
    }
 
    /**
     *  Creates a new Mass component representing an object of the specified mass
     *  and the default object type of TYPE_SOLID.  
     */   
    public Mass( float mass ) {
        this(mass, TYPE_SOLID);
    }

    /**
     *  Creates a new Mass component representing an object of the specified
     *  material type and the specified mass.  Material type can be used to filter
     *  impacts and collisions between objects, either as specific IDs or as mask
     *  values depending on the calling code's needs.
     */    
    public Mass( float mass, int type ) {
        this.mass = mass;
        this.type = type;        
    }
 
    /**
     *  Returns the physical mass of the entity to which this component is applied.
     *  This will control the energy tranfer between colliding objects.
     */   
    public float getMass() {
        return mass; 
    }
 
    /**
     *  Returns the type of mass of the entity to which this component is applied.
     *  This may be used to filter different types of impacts depending on the needs
     *  of the calling code.
     */
    public int getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "Mass[" + mass + ":0b" + Integer.toBinaryString(type) + "]";
    }     
       
}


