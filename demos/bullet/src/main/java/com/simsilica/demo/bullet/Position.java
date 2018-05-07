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

import java.util.Objects;

import com.google.common.base.MoreObjects;

import com.jme3.math.*;

import com.simsilica.es.EntityComponent;
import com.simsilica.mathd.*;


/**
 *  Represents game object position in the demo.
 *
 *  @author    Paul Speed
 */
public class Position implements EntityComponent {
    private Vector3f location;
    private Quaternion orientation;
    
    protected Position() {
    }
    
    public Position( double x, double y, double z ) {
        this(new Vector3f((float)x, (float)y, (float)z), new Quaternion());
    }
    
    public Position( Vector3f location ) {
        this(location, new Quaternion());
    }
    
    public Position( Vec3d location, double facing ) {
        this(location.toVector3f(), facing);
    }

    public Position( Vec3d location, Quatd orientation ) {
        this(location.toVector3f(), orientation.toQuaternion());
    }
    
    public Position( Vector3f location, double facing ) {
        this(location, new Quaternion().fromAngles(0, (float)facing, 0));
    }
    
    public Position( Vector3f location, Quaternion orientation ) {
        this.location = location;
        this.orientation = orientation;
    }
  
    public Vector3f getLocation() {
        return location;
    }
    
    public Quaternion getOrientation() {
        return orientation;
    }
     
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("location", location)
            .add("orientation", orientation)
            .toString();
    }
}
