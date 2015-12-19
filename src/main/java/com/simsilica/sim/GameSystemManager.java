/*
 * $Id$
 * 
 * Copyright (c) 2015, Simsilica, LLC
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

package com.simsilica.sim;

import com.simsilica.event.EventBus;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 *  Manages the life-cycle of a set of GameSystems.  GameSystems can
 *  also use the manager to look up peer systems that they depend on.
 *  Note: generally all of the systems will be setup ahead of time
 *  before a particular GameSystemManager is initialized() and started().
 *  For use cases where the systems are added or removed at runtime, it
 *  is up to the caller to manage threading and any contention that might
 *  occur.  GameSystem.initialize() and GameSystem.start(), when run in
 *  a multithreaded environment, are called from the thread that adds them
 *  to the manager.
 *
 *  <p>In general, GameSystemManager should be treated as a single-threaded
 *  class and used accordingly.  No internal threading protection is provided
 *  except by systems that specifically provide that or by the 'enqueue()' method
 *  that allows inserting calls into the game loop from separate threads.</p>
 *
 *  @author    Paul Speed
 */
public class GameSystemManager {
 
    private final Map<Class, Object> index = new HashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private GameSystem[] systemArray = null;
    private boolean initialized;
    private boolean started;
    private final SimTime stepTime = new SimTime();
    private final SimEvent simEvent = new SimEvent(this); // can reuse it
 
    public GameSystemManager() {
        register(TaskDispatcher.class, new TaskDispatcher());    
    }

    /**
     *  Enqueues a task that will be run at the beginning of the next
     *  update() call on the update thread.  This delegates to the
     *  TaskDispatcher system registered to this GameSystemManager.
     */
    public <V> Future<V> enqueue( Callable<V> callable ) {
        TaskDispatcher dispatcher = get(TaskDispatcher.class);
        if( dispatcher == null ) {
            throw new RuntimeException("No TaskDispatcher registered");
        }
        return dispatcher.enqueue(callable);
    }
 
    private GameSystem[] getArray() {
        if( systemArray != null ) {
            return systemArray;
        }
        systemArray = systems.toArray(new GameSystem[systems.size()]);
        return systemArray;
    }
 
    public void initialize() {
        if( initialized ) {
            throw new RuntimeException("Already initialized.");
        }
        EventBus.publish(SimEvent.simInitializing, simEvent);
        for( GameSystem sys : getArray() ) {
            sys.initialize(this);
        }
        this.initialized = true;        
        EventBus.publish(SimEvent.simInitialized, simEvent);
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void terminate() {
        if( !initialized ) {
            return;
        }
        EventBus.publish(SimEvent.simTerminating, simEvent);
        for( GameSystem sys : getArray() ) {
            sys.terminate(this);
        }
        this.initialized = false;
        EventBus.publish(SimEvent.simTerminated, simEvent);
    }
 
    public void start() {
        if( !initialized ) {
            throw new RuntimeException("Not initialized");
        }
        if( started ) {
            return;
        }
        EventBus.publish(SimEvent.simStarting, simEvent);
        for( GameSystem sys : getArray() ) {
            sys.start();
        }
        this.started = true;
        EventBus.publish(SimEvent.simStarted, simEvent);
    }
 
    public boolean isStarted() {
        return started;
    }
    
    public void stop() {
        if( !started ) {
            return;
        }
        EventBus.publish(SimEvent.simStopping, simEvent);
        for( GameSystem sys : getArray() ) {
            sys.stop();
        }
        this.started = false;
        EventBus.publish(SimEvent.simStopped, simEvent);
    }
 
    protected void attachSystem( GameSystem system ) {
        systems.add(system);
        systemArray = null; 
        if( initialized ) {
            system.initialize(this);
        }
        if( started ) {
            system.start();
        }  
    } 

    protected void detachSystem( GameSystem system ) {
        systems.remove(system);
        systemArray = null;   
        if( started ) {
            system.stop();
        }
        if( initialized ) {
            system.terminate(this);
        }
    } 
 
    /**
     *  Adds a system without index registration.  Useful
     *  for cases where type lookup is neither desired or
     *  needed.
     */
    public void addSystem( GameSystem system ) {
        attachSystem(system);
    }
    
    /**
     *  Removes a previously added system.
     */
    public void removeSystem( GameSystem system ) {
        index.values().remove(system); // just in case
        detachSystem(system);
    }
    
    /**
     *  Returns a system-level object preoviously registered using the
     *  register() method.
     */
    public <T> T get( Class<T> type ) {
        Object result = index.get(type);
        return type.cast(result);
    }
    
    /**
     *  Registers a system-level object that will be associated with the
     *  specified type for later retrieval.  If the object implements GameSystem
     *  then it is automatically registered as a system as if addSystem() were
     *  called.
     */
    public <T, S extends T> T register( Class<T> type, S object ) {
        Object previous = index.put(type, object);
        if( previous != null && previous instanceof GameSystem ) {
            detachSystem((GameSystem)previous);
        }
        if( object instanceof GameSystem ) {
            attachSystem((GameSystem)object);
        }
        return type.cast(object);
    }
 
    /**
     *  Updates the current SimTime and calls update on all of
     *  the systems.  It is up to the application to periodically
     *  call this method, either by setting up a GameLoop thread or
     *  by calling it in an AppState or other parts of the application's
     *  normal update loop.
     */
    public void update() {
        // Update the step time...
        long time = System.nanoTime(); 
        stepTime.update(time);
        
        // Update the systems.
        for( GameSystem sys : getArray() ) {
            sys.update(stepTime);
        }
    } 
}


