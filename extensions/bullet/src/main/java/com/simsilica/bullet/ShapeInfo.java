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
 *  The ID of the collision shape associated with some entity.
 *  
 *  @author    Paul Speed
 */
public class ShapeInfo implements EntityComponent {

    private int id;

    protected ShapeInfo() {
    }
 
    /**
     *  Creates a new ShapeInfo component with the specified integer ID, usually
     *  representing some specific string in the EntityData's string index.
     */   
    public ShapeInfo( int id ) {
        this.id = id;
    }
 
    /**
     *  Returns the integer ID of the collision shape associated with the 
     *  entity to which this component applies.  Usually this is an ID in the
     *  EntityData's strings table.
     */   
    public int getShapeId() {
        return id; 
    }
 
    /**
     *  Returns the shape ID name if it exists in the EntityData's string table.
     */
    public String getShapeName( EntityData ed ) {
        return ed.getStrings().getString(id);
    }
 
    /**
     *  Creates a ShapeInfo for the specified name by reusing or generating
     *  a unique ID as required.
     */   
    public static ShapeInfo create( String name, EntityData ed ) {
        return new ShapeInfo(ed.getStrings().getStringId(name, true));
    }
 
    /**
     *  Returns the same string representation as toString() but if EntityData is
     *  non-null then the ID will be resolved to a string.  If EntityData is null
     *  then this is the same as calling toString().
     */   
    public String toString( EntityData ed ) {
        String s = ed == null ? String.valueOf(id) : getShapeName(ed);
        return getClass().getSimpleName() + "[" + s + "]";  
    }
    
    @Override
    public String toString() {
        return toString(null);
    } 
}
