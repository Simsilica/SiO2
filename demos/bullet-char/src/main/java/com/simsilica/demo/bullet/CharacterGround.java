/*
 * $Id$
 * 
 * Copyright (c) 2019, Simsilica, LLC
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
import com.simsilica.mathd.*;

/**
 *  Points to the ground entity that a character is sitting on
 *  and denotes that ground's velocity.
 *
 *  @author    Paul Speed
 */
public class CharacterGround implements EntityComponent {

    private EntityId groundId;
    
    // It's possible that we will want to calculate this in the view layer
    // but I want to see if the induced delay might add a little 
    // character (pun intended).  Also, there may be more than one ground
    // contact but the ground velocity is nicely averaged for us.  
    private Vec3d velocity;
    
    public CharacterGround() {
    }
    
    public CharacterGround( EntityId groundId, Vec3d velocity ) {   
        this.groundId = groundId;
        this.velocity = velocity;
    }
 
    public Vec3d getVelocity() {
        return velocity;
    }
    
    public EntityId getCharacterGroundId() {
        return groundId;
    }

    @Override
    public String toString() {
        return "CharacterGround[groudId=" + groundId + ", groundVelocity=" + velocity + "]";
    }      
}

