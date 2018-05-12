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

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;

/**
 *  Indicate that a physics body should be a ghost.  If there is a parent, 
 *  then this ghost entity is moved with the parent and its SpawnPosition
 *  is relative to the parent.  
 *
 *  @author    Paul Speed
 */
public class Ghost implements EntityComponent {
 
    /**
     *  Indicates that the ghost will generate collisions against non-static
     *  rigid bodies.
     */
    public static final byte COLLIDE_DYNAMIC = 0x01;
     
    /**
     *  Indicates that the ghost will generate collisions against static
     *  rigid bodies.
     */
    public static final byte COLLIDE_STATIC = 0x02;
    
    /**
     *  Indicates that the ghost will generate collisions against other ghosts.
     *  Note: if either ghost has this collision flag then a collision will be
     *  generated so it's possible that ghosts that do not want collisions with
     *  other ghosts may still see them.
     */
    public static final byte COLLIDE_GHOST = 0x04;

    /**
     *  Indicates that the ghost will generate collisions against all rigid
     *  bodies, static or otherwise.
     */
    public static final byte COLLIDE_BODY = COLLIDE_DYNAMIC | COLLIDE_STATIC;
         
    /**
     *  Indicates the the ghost will generate collisions against any object.
     */
    public static final byte COLLIDE_ALL = (byte)0xff;
    
    private EntityId parent;
    private byte collisionMask;
 
    /**
     *  Creates a ghost that will collide with other physical bodies, but not
     *  other ghosts.
     */   
    public Ghost() {
        this(null, COLLIDE_BODY);
    }
    
    /**
     *  Creates a ghost that follows the parent and that will collide with other 
     *  physical bodies, but not other ghosts.
     */   
    public Ghost( EntityId parent ) {
        this(parent, COLLIDE_BODY);
    }

    public Ghost( byte collisionMask ) {
        this(null, collisionMask);
    }
    
    public Ghost( EntityId parent, byte collisionMask ) {
        this.parent = parent;
        this.collisionMask = collisionMask;
    }
 
    public EntityId getParentEntity() {
        return parent;
    }
 
    public byte getCollisionMask() {
        return collisionMask;
    }
 
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
            .omitNullValues()
            .add("parent", parent)
            .add("collisionMask", "0b" + Integer.toBinaryString(collisionMask))
            .toString();
    }
}
