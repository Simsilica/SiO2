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

import com.simsilica.event.ErrorEvent;
import com.simsilica.event.EventBus;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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

    static Logger log = LoggerFactory.getLogger(GameSystemManager.class);

    enum State { Terminated, Initializing, Initialized, Starting, Started, Stopping, Stopped, Terminating };

    private State state = State.Terminated;
    private final Map<Class, Object> index = new HashMap<>();
    private final List<GameSystem> systems = new ArrayList<>();
    private GameSystem[] systemArray = null;
    private final SimTime stepTime = new SimTime();
    private final SimEvent simEvent = new SimEvent(this); // can reuse it
    private SystemTiming timing;

    // Keep track of the systems that were actually initialized
    // and actually started so that we can clean them up on stop() and
    // terminate() even if full startup failed.
    private final List<GameSystem> initialized = new ArrayList<>();
    private final List<GameSystem> started = new ArrayList<>();

    public GameSystemManager() {
        register(TaskDispatcher.class, new TaskDispatcher());
        register(Blackboard.class, new Blackboard());
    }

    /**
     *  If set then the specified timing will collect per-frame, per-system
     *  timing information.
     */
    public void setSystemTiming( SystemTiming timing ) {
        this.timing = timing;
    }

    public SystemTiming getSystemTiming() {
        return timing;
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
        log.trace("initialize()");
        if( state != State.Terminated ) {
            throw new RuntimeException("Already initialized.");
        }
        state = State.Initializing;
        EventBus.publish(SimEvent.simInitializing, simEvent);
        try {
            for( GameSystem sys : getArray() ) {
                if( log.isTraceEnabled() ) {
                    log.trace("initializing:" + sys);
                }
                sys.initialize(this);
                initialized.add(sys);
            }
            state = State.Initialized;
            EventBus.publish(SimEvent.simInitialized, simEvent);
        } catch( RuntimeException e ) {
            EventBus.publish(SimEvent.simFailed, simEvent);

            // Cleanup what we can
            if( !initialized.isEmpty() ) {
                log.warn("Terminating " + initialized.size() + " initialized systems from partial initialization.");
                terminateSystems();
            }

            // Rethrow the exception
            throw e;
        }
    }

    public boolean isInitialized() {
        switch( state ) {
            case Initialized:
            case Starting:
            case Started:
            case Stopping:
            case Stopped:
                return true;
            default:
                return false;
        }
    }

    public boolean isStarted() {
        switch( state ) {
            case Started:
                return true;
            default:
                return false;
        }
    }

    public void terminate() {
        log.trace("terminate()");
        if( !isInitialized() ) {
            throw new RuntimeException("Not initialized.  State:" + state);
        }
        state = State.Terminating;
        EventBus.publish(SimEvent.simTerminating, simEvent);
        terminateSystems();
        state = State.Terminated;
        EventBus.publish(SimEvent.simTerminated, simEvent);
    }

    public void start() {
        log.trace("start()");
        if( !isInitialized() ) {
            throw new RuntimeException("Not initialized");
        }
        if( isStarted() ) {
            return;
        }

        // Update to the latest time
        updateTime();

        state = State.Starting;
        EventBus.publish(SimEvent.simStarting, simEvent);
        try {
            for( GameSystem sys : getArray() ) {
                if( log.isTraceEnabled() ) {
                    log.trace("starting:" + sys);
                }
                sys.start();
                started.add(sys);
            }
            state = State.Started;
            EventBus.publish(SimEvent.simStarted, simEvent);
        } catch( RuntimeException e ) {
            log.error("Error starting systems", e);
            EventBus.publish(SimEvent.simFailed, simEvent);
            EventBus.publish(ErrorEvent.fatalError, new ErrorEvent(e));

            // Cleanup what we can
            if( !started.isEmpty() ) {
                log.warn("Stopping " + started.size() + " started systems from partial startup.");
                stopSystems();
            }

            if( !initialized.isEmpty() ) {
                log.warn("Terminating " + initialized.size() + " initialized systems from partial startup.");
                terminateSystems();
            }

            // Rethrow the exception
            throw e;
        }
    }

    public void stop() {
        log.trace("stop()");
        if( !isStarted() ) {
            return;
        }
        state = State.Stopping;
        EventBus.publish(SimEvent.simStopping, simEvent);
        stopSystems();
        state = State.Stopped;
        EventBus.publish(SimEvent.simStopped, simEvent);
    }

    protected void terminateSystems() {
        for( GameSystem sys : initialized ) {
            if( log.isTraceEnabled() ) {
                log.trace("terminating:" + sys);
            }
            sys.terminate(this);
        }
    }

    protected void stopSystems() {
        for( GameSystem sys : started ) {
            if( log.isTraceEnabled() ) {
                log.trace("stopping:" + sys);
            }
            sys.stop();
        }
    }

    protected void attachSystem( GameSystem system ) {
        systems.add(system);
        systemArray = null;
        if( isInitialized() || state == State.Initializing ) {
            if( log.isTraceEnabled() ) {
                log.trace("initializing:" + system);
            }
            system.initialize(this);
        }
        if( isStarted() || state == State.Starting ) {
            if( log.isTraceEnabled() ) {
                log.trace("starting:" + system);
            }
            system.start();
        }
    }

    protected void detachSystem( GameSystem system ) {
        systems.remove(system);
        systemArray = null;
        if( isStarted() && state != State.Stopping ) {
            if( log.isTraceEnabled() ) {
                log.trace("stopping:" + system);
            }
            system.stop();
        }
        if( isInitialized() && state != State.Terminating ) {
            if( log.isTraceEnabled() ) {
                log.trace("terminating:" + system);
            }
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
        return get(type, false);
    }

    /**
     *  Returns a system-level object preoviously registered using the
     *  register() method.  If failOnMiss is true then an IllegalArgumentException
     *  is thrown if the system is not registered.
     */
    public <T> T get( Class<T> type, boolean failOnMiss ) {
        Object result = index.get(type);
        if( result == null && failOnMiss ) {
            throw new IllegalArgumentException("System not found for:" + type);
        }
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
        try {
            // Update the step time...
            updateTime();

            String frame = String.format("%06d", stepTime.getFrame());
            try( MDC.MDCCloseable temp = MDC.putCloseable("frame", frame) ) {
                if( timing != null ) {
                    timing.startFrame();
                    for( GameSystem sys : getArray() ) {
                        try( AutoCloseable info = timing.trackUpdate(sys) ) {
                            sys.update(stepTime);
                        }
                    }
                    timing.endFrame();
                } else {
                    // Update the systems.
                    for( GameSystem sys : getArray() ) {
                        sys.update(stepTime);
                    }
                }
            }
        } catch( Throwable t ) {
            log.error("Error updating systems", t);
            // Treat this as a fatal error... systems should
            // handle their own errors otherwise
            EventBus.publish(ErrorEvent.fatalError, new ErrorEvent(t));
        }
    }

    protected void updateTime() {
        long time = System.nanoTime();
        stepTime.update(time);
    }

    /**
     *  Returns the SimTime representing the time at the beginning of the
     *  most recent update() call.  Note: this is not thread safe if the
     *  GameSystemManager is being updated from a background thread like
     *  GameLoop.  GameLoop provides its own stable thread-safe step time.
     */
    public SimTime getStepTime() {
        return stepTime;
    }
}


