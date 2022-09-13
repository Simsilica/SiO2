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

package com.simsilica.state;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import com.simsilica.sim.*;

/**
 *  Manages a GameSystemManager for managing single player or client-specific
 *  SiO2 game systems.  Can be run on a background thread or inline with the
 *  main rendering loop.  (Runs on its own thread by default.)  
 *
 *  @author    Paul Speed
 */
public class GameSystemsState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(GameSystemsState.class);

    private GameSystemManager systems;
    private GameLoop loop;
    private boolean background;
    private long updateRateNanos = GameLoop.FPS_60; // when running in its own thread.

    public GameSystemsState() {
        this(new GameSystemManager(), true);
    }

    public GameSystemsState( GameSystemManager systems ) {
        this(systems, true);
    }

    public GameSystemsState( boolean background ) {
        this(new GameSystemManager(), background);
    }

    public GameSystemsState( GameSystemManager systems, boolean background ) {
        this.systems = systems;
        this.background = background;
    }

    /**
     *  Sets the period of time between frame updates.  This only applies
     *  when background mode is true and the state has not yet been initialized.
     */
    public void setUpdateRate( long nanos ) {
        this.updateRateNanos = nanos;
    }
    
    public long getUpdateRate() {
        return updateRateNanos;
    }

    public SimTime getStepTime() {
        return systems.getStepTime();
    }

    public long getGameTime() {
        return systems.getStepTime().getTime();
    }

    public void addSystem( GameSystem system ) {
        systems.addSystem(system);
    }

    public void removeSystem( GameSystem system ) {
        systems.removeSystem(system);
    }

    public <T> T get( Class<T> type ) {
        return systems.get(type);
    }

    public <T, S extends T> T register( Class<T> type, S object ) {
        return systems.register(type, object);
    }

    @Override
    protected void initialize( Application app ) {
        if( background ) {
            if( loop == null ) {
                loop = new GameLoop(systems, updateRateNanos);
            }        
        } else {
            systems.initialize();
        }
    }

    @Override
    protected void cleanup( Application app ) {
        if( !background ) {
            systems.terminate();
        }
    }

    @Override
    protected void onEnable() {
        if( background ) {
            loop.start();
        } else {
            systems.start();
        }
    }

    @Override
    public void update( float tpf ) {
        if( !background ) {
            systems.update();
        }
    }

    @Override
    protected void onDisable() {
        if( background ) {
            loop.stop();
        } else {
            systems.stop();
        }
    }
}
