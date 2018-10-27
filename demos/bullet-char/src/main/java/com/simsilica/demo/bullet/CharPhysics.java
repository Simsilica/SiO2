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

import com.jme3.math.*;


/**
 *  Sets the character's physics settings that control things
 *  like jump force, whether they can move when falling, etc..
 *
 *  @author    Paul Speed
 */
public class CharPhysics {    

    /**
     *  Sets the gravity for the character.
     */
    public Vector3f gravity = new Vector3f(0, -20, 0);
    
    /**
     *  Sets the acceleration force used for movement while in
     *  ground contact.
     */   
    public float groundImpulse = 200;
    
    /**   
     *  Sets the acceleration force used for movement while NOT in
     *  ground contact.
     */   
    public float airImpulse = 50;
    
    /**
     *  Sets the vertical jump velocity.
     */ 
    public float jumpForce = 10;
    
    /**
     *  When true, releasing the jump button during a jump will
     *  kill the jump early.
     */
    public boolean shortJumps = true;
    
    /**
     *  When true, holding the jump button will cause the player
     *  to automatically rejump when in contact with the ground again.
     */
    public boolean autoBounce = false;
    
    public CharPhysics() {
    }       
}

