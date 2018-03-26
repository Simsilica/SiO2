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

package com.simsilica.sim.common;

import org.slf4j.*;

import com.simsilica.es.*;
import com.simsilica.es.common.*;
import com.simsilica.sim.*;

/**
 *  Tracks entities with Decay components and removes them once
 *  the decay has expired. 
 *
 *  @author    Paul Speed
 */
public class DecaySystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(DecaySystem.class);

    private EntityData ed;

    private DecayContainer entities;

    public DecaySystem() {
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);        
        entities = new DecayContainer(ed);
    }
    
    @Override
    protected void terminate() {
    }

    @Override
    public void start() {
        super.start();
        entities.start();
    }

    /**
     *  Called when a decaying entity has expired.  Default 
     *  implementation calls EntityData.removeEntity().
     */
    protected void destroyEntity( Entity e ) {
        if( log.isTraceEnabled() ) {
            log.trace("Removing:" + e);
        }
        ed.removeEntity(e.getId());
    }

    @Override
    public void update( SimTime time ) {
        super.update(time);
        entities.update();
        
        long current = time.getTime();
        
        // Check for expired entities
        for( Entity e : entities.getArray() ) {
            Decay d = e.get(Decay.class);
            if( d.isDead(current) ) {
                destroyEntity(e);
            }
        }
    }

    @Override
    public void stop() {
        entities.stop();
        super.stop();
    }    
 
    /**
     *  A simple EntityContainer that just tracks membership of the
     *  entity based on the Decay component.  Technically we're not much better
     *  here than a straight EntitySet except we get the array access for
     *  free and convenient start/stop behavior.  (Could be a standard utility
     *  extension, I guess.)
     */    
    private class DecayContainer extends EntityContainer<Entity> {

        public DecayContainer( EntityData ed ) {
            super(ed, Decay.class);
        }
 
        @Override       
        public Entity[] getArray() {
            return super.getArray();
        }
 
        @Override
        protected Entity addObject( Entity e ) {
            return e;
        }

        @Override
        protected void updateObject( Entity object, Entity e ) {            
        }
        
        @Override
        protected void removeObject( Entity object, Entity e ) {
        }    
    }
    
}

