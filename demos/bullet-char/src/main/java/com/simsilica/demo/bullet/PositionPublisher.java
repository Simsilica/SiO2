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

import com.simsilica.bullet.EntityPhysicsObject;
import com.simsilica.bullet.PhysicsObjectListener;

import com.simsilica.es.EntityData;
import com.simsilica.sim.SimTime;


/**
 *  Directly publishes all physics object state to the entity as a Position.
 *
 *  @author    Paul Speed
 */
public class PositionPublisher implements PhysicsObjectListener {

    private EntityData ed;

    public PositionPublisher( EntityData ed ) {
        this.ed = ed;
    }
    
    public void startFrame( SimTime time ) {
    }
    
    public void endFrame() {
    }
    
    public void added( EntityPhysicsObject object ) {
//System.out.println("added(" + object + ")");    
    }
    
    public void updated( EntityPhysicsObject object ) {
//System.out.println("update(" + object + ")");    
        Position pos = new Position(object.getPhysicsLocation(null), object.getPhysicsRotation(null));
//System.out.println("  pos:" + pos);        
        ed.setComponents(object.getId(), pos); 
    }
    
    public void removed( EntityPhysicsObject object ) {
//System.out.println("removed(" + object + ")");    
    }
}


