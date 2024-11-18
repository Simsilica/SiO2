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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;

/**
 *  A standard 'background thread' game loop implementation that
 *  can control the lifecycle of a GameSystemManager and call its
 *  update() method at a particular frequency.
 *
 *  @author    Paul Speed
 */
public class GameLoop {

    public static final long FPS_60 = 16666667L;

    static Logger log = LoggerFactory.getLogger(GameLoop.class);

    private final GameSystemManager systems;
    private final Runner loop = new Runner();

    private long updateRate;
    private LoopSleepStrategy sleepStrategy = new LegacyLoopSleepStrategy(FPS_60);

    /**
     *  Keep track of the last update time in a thread safe way.
     */
    private AtomicLong lastStepTime = new AtomicLong();

    /**
     *  Convenient wrapper object to provide sim time outside
     *  of the run thread.
     */
    private SimTime safeTime = new SimTime();

    private Integer priority;
    private int defaultPriority = loop.getPriority();

    public GameLoop( GameSystemManager systems ) {
        this(systems, FPS_60); // 60 FPS
    }

    public GameLoop( GameSystemManager systems, long updateRateNanos ) {
        this(systems, updateRateNanos, null);
    }

    public GameLoop( GameSystemManager systems, long updateRateNanos, Long idleSleepTime ) {
        this.systems = systems;
        this.updateRate = updateRateNanos;
        sleepStrategy.setUpdateRateNanos(updateRateNanos);
        setIdleSleepTime(idleSleepTime);
    }

    public GameSystemManager getGameSystemManager() {
        return systems;
    }

    /**
     *  Returns the SimTime representing the time at the begining of the
     *  last call to update.  This SimTime is isolated from the thread and
     *  will update itself when this method is called.  That means the value
     *  is stable as long as it is used from the same thread.
     */
    public SimTime getStepTime() {
        // Update only when needed so that tpf, etc. are accurate.
        if( safeTime.getTime() != lastStepTime.get() ) {
            //safeTime.update(lastStepTime.get());
            // Just force the current time to be the last step time.
            // safeTime's tpf is generally going to be nonsense anyway, so
            // we'll leave it unset.  Callers that need decoupled tpf will
            // have to decide what they want to base it on.
            safeTime.setCurrentTime(lastStepTime.get());
        }
        return safeTime;
    }

    public void setPriority( Integer priority ) {
        this.priority = priority;
        if( priority == null ) {
            loop.setPriority(defaultPriority);
        } else {
            loop.setPriority(priority);
        }
    }

    public Integer getPriority() {
        return priority;
    }

    /**
     *  Starts the background game loop thread and initializes and
     *  starts the game system manager (if it hasn't been initialized or started already).
     *  The systems will be initialized and started on the game loop background
     *  thread.
     */
    public void start() {
        start(false);
    }

    /**
     *  Starts the background game loop thread and initializes and
     *  starts the game system manager (if it hasn't been initialized or started already).
     *  The systems will be initialized and started on the game loop background
     *  thread.  If "wait" is true then start() won't return until the
     *  game systems have been initialized and started.  In that case, this method
     *  will also throw an error if the game loop startup failed for some reason.
     */
    public void start( boolean wait ) {
        loop.start();
        if( wait ) {
            loop.waitForInitialized();
        }
    }

    /**
     *  Stops the background game loop thread, stopping and terminating
     *  the game systems.  This method will wait until the thread has been
     *  fully shut down before returning.
     *  The systems will be stopped and terminated on the game loop background
     *  thread.
     */
    public void stop() {
        loop.close();
    }

    /**
     *  Sets the period of time in milliseconds that the update loop will
     *  wait between time interval checks.  This defaults to 0 because on
     *  Windows the default 60 FPS update rate will cause frame drops for
     *  a idle sleep time of 1 or more.  However, 0 causes the loop to consume
     *  a noticable amount of CPU.  For lower framerates, 1 is recommended
     *  and will be set automically based on the current frame rate interval
     *  if null is specified.
     *
     *  @deprecated Use get/setLoopSleepStrategy() instead.
     */
    @Deprecated
    public final void setIdleSleepTime( Long millis ) {
        long idleSleepTime;
        if( millis == null ) {
            // Configure a reasonable default
            if( updateRate > FPS_60 ) {
                // Can probably get away with more sleep
                idleSleepTime = 1;
            } else {
                // Else on Windows, sleep > 0 will take almost as long as
                // a frame
                idleSleepTime = 0;
            }
        } else {
            idleSleepTime = millis;
        }
        if( sleepStrategy instanceof LegacyLoopSleepStrategy ) {
            // Still support the old behavior
            ((LegacyLoopSleepStrategy)sleepStrategy).setIdleSleepTime(idleSleepTime);
        } else {
            log.warn("Directly setting idleSleepTime only works with legacy sleep strategy");
        }
    }

    public Long getIdleSleepTime() {
        if( sleepStrategy instanceof LegacyLoopSleepStrategy ) {
            // Still support the old behavior
            return ((LegacyLoopSleepStrategy)sleepStrategy).getIdleSleepTime();
        }
        return null;
    }

    /**
     *  Sets the sleep strategy object that determines how the game loop
     *  sleeps between loop iterations.
     */
    public void setLoopSleepStrategy( LoopSleepStrategy sleepStrategy ) {
        if( sleepStrategy == null ) {
            throw new IllegalArgumentException("Sleep strategy cannot be null");
        }
        this.sleepStrategy = sleepStrategy;
        sleepStrategy.setUpdateRateNanos(updateRate);
    }

    public LoopSleepStrategy getLoopSleepStrategy() {
        return sleepStrategy;
    }

    /**
     *  Use our own thread instead of a java executor because we need
     *  more control over the update loop.  ScheduledThreadPoolExecutor will
     *  try to call makeup frames if it gets behind and we'd rather just drop
     *  them.  Furthermore, this allows us to 'busy wait' for the next 'frame'.
     */
    protected class Runner extends Thread {
        private final AtomicBoolean go = new AtomicBoolean(true);

        private Throwable startupFailure;
        private boolean initialized;

        public Runner() {
            setName( "GameLoopThread" );
        }

        public void close() {
            go.set(false);
            try {
                join();
            } catch( InterruptedException e ) {
                throw new RuntimeException("Interrupted while waiting for game loop thread to complete.", e);
            }
        }

        public synchronized void waitForInitialized() {
            while( !initialized && startupFailure == null ) {
                try {
                    wait();
                } catch( InterruptedException e ) {
                    throw new RuntimeException("Interrupted waiting for initialize", e);
                }
            }
            if( startupFailure != null ) {
                throw new RuntimeException("Failed to initialize game loop thread", startupFailure);
            }
        }

        protected synchronized void initialized() {
            initialized = true;
            notifyAll();
        }

        protected synchronized void failed( Throwable t ) {
            startupFailure = t;
            notifyAll();
        }

        @Override
        public void run() {

            try {
                if( !systems.isInitialized() ) {
                    systems.initialize();
                }
                if( !systems.isStarted() ) {
                    systems.start();
                }
                initialized();
            } catch( RuntimeException e ) {
                log.error("Error starting game loop", e);
                failed(e);

                // Still kill the thread
                return;
            }

            long lastTime = System.nanoTime();
            long lastUpdateDelta = 0;
            while( go.get() ) {
                long time = System.nanoTime();
                long delta = time - lastTime;

                if( delta >= updateRate ) {
                    // Time to update
                    lastTime = time;
                    systems.update();

                    // We keep lastUpdateDelta 'outside' the loop because we may
                    // loop several times between actual calls to udpate but the loopSleep()
                    // method may still want to know how long the last one took.
                    lastUpdateDelta = System.nanoTime() - time;
                    lastStepTime.set(systems.getStepTime().getTime());
                    continue;
                }
                try {
                    // I will certainly regret so many 'long' parameters someday.
                    sleepStrategy.loopSleep(time, delta, lastTime, lastUpdateDelta);
                } catch( InterruptedException e ) {
                    throw new RuntimeException("Interrupted sleeping", e);
                }
            }

            // Stop the systems
            systems.stop();

            // Terminate the systems
            systems.terminate();
        }
    }
}

