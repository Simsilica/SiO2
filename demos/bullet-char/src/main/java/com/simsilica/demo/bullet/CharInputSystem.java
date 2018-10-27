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
import com.simsilica.sim.*;

import com.simsilica.bullet.*;

/**
 *  A system that makes entities with the CharInput component
 *  move.
 *
 *  @author    Paul Speed
 */
public class CharInputSystem extends AbstractGameSystem {

    private EntityData ed;
    private BulletSystem bullet;

    private CharInputContainer characters;

    // Right now there is only one for all characters... which 
    // is generally only players anyway.
    private CharPhysics charPhysics = new CharPhysics();
    private boolean refreshSettings = false;

    public CharInputSystem() {
    }

    public void setCharPhysics( CharPhysics charPhysics ) {
        this.charPhysics = charPhysics;
        
        // Update the drivers... this is not really thread safe
        this.refreshSettings = true;
    }

    public CharPhysics getCharPhysics() {
        return charPhysics;
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class, true);
        bullet = getSystem(BulletSystem.class, true);
        
        characters = new CharInputContainer(ed);
    }
    
    @Override
    protected void terminate() {
    }

    @Override
    public void start() {
        super.start();
        characters.start();
    }


    @Override
    public void update( SimTime time ) {
        super.update(time);
        if( refreshSettings ) {
            refreshSettings = false;
            for( CharInputDriver driver : characters.getArray() ) {
                driver.setCharPhysics(charPhysics);
            }
        }
        characters.update();
    }

    @Override
    public void stop() {
        characters.stop();
        super.stop();
    }    
     
    private class CharInputContainer extends EntityContainer<CharInputDriver> {

        public CharInputContainer( EntityData ed ) {
            super(ed, CharInput.class, SpawnPosition.class, ShapeInfo.class, Mass.class);
        }
 
        @Override       
        public CharInputDriver[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected CharInputDriver addObject( Entity e ) {
        
            CharInputDriver driver = new CharInputDriver(e, charPhysics);
            bullet.setControlDriver(e.getId(), driver);
 
            updateObject(driver, e);
        
            return driver;
        }

        @Override
        protected void updateObject( CharInputDriver object, Entity e ) {
            object.setInput(e.get(CharInput.class));
        }
        
        @Override
        protected void removeObject( CharInputDriver object, Entity e ) {
            bullet.setControlDriver(e.getId(), null);
        }
    
    }
    
}

