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

import com.jme3.math.Vector3f;

import com.simsilica.es.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class Impulse implements EntityComponent {
    private Vector3f linear;
    private Vector3f angular;

    protected Impulse() {
    }

    public Impulse( float x, float y, float z ) {
        this(new Vector3f(x, y, z), null);
    }
    
    public Impulse( Vector3f linear ) {
        this(linear, null);
    }
    
    public Impulse( Vector3f linear, Vector3f angular ) {
        this.linear = linear;
        this.angular = angular;
    }
    
    public Vector3f getLinearVelocity() {
        return linear; 
    }
 
    public Vector3f getAngularVelocity() {
        return angular; 
    }
   
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("linearVelocity", linear)
            .add("angularVelocity", angular)
            .toString();
    }     
}



/**
 *  Applies a specific velocity to an object.  This is not a true
 *  impulse in the "physical" sense where an impulse is a force over
 *  a period of time.  In this case, we directly apply the velocity
 *  since that is a more common thing to want to do to an object...
 *  instead of making the caller work out the F = ma math that would result
 *  in that velocity change.  Note that this impulse will also totally
 *  override existing velocity.
 *
 *  @author    Paul Speed
 *//*
public class Impulse implements EntityComponent {

    private Vector3f linear;
    private Vector3f angular;

    protected Impulse() {
    }

    public Impulse( float x, float y, float z ) {
        this(new Vector3f(x, y, z), null);
    }
    
    public Impulse( Vector3f linear ) {
        this(linear, null);
    }
    
    public Impulse( Vector3f linear, Vector3f angular ) {
        this.linear = linear;
        this.angular = angular;
    }
    
    public Vector3f getLinearVelocity() {
        return linear; 
    }
 
    public Vector3f getAngularVelocity() {
        return angular; 
    }
   
    @Override
    public String toString() {
        return "Impulse[linearVelocity=" + linear + ", angularVelocity=" + angular + "]";
    }     
}*/
