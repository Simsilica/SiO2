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


/**
 *  A standard 'background thread' game loop implementation that
 *  can control the lifecycle of a GameSystemManager and call its
 *  update() method at a particular frequency.
 *
 *  @author    Paul Speed
 */
public class GameLoop {
    private final GameSystemManager systems;
    private final Runner loop = new Runner(); 
 
    private long updateRate;
    
    public GameLoop(GameSystemManager systems) {
        this(systems, 16666667L); // 60 FPS 
    }
    
    public GameLoop( GameSystemManager systems, long updateRateNanos ) {
        this.systems = systems;
        this.updateRate = updateRateNanos;
    }   

    public GameSystemManager getGameSystemManager() {
        return systems;
    }

    /**
     *  Starts the background game loop thread and initializes and
     *  starts the game system manager (if it hasn't been initialized or started already).
     *  The systems will be initialized and started on the game loop background
     *  thread.
     */
    public void start() {
        loop.start();
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
     *  Use our own thread instead of a java executor because we need
     *  more control over the update loop.  ScheduledThreadPoolExecutor will
     *  try to call makeup frames if it gets behind and we'd rather just drop
     *  them.  Furthermore, this allows us to 'busy wait' for the next 'frame'.
     */   
    protected class Runner extends Thread {
        private final AtomicBoolean go = new AtomicBoolean(true);
        
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
        
        @Override
        public void run() {
        
            if( !systems.isInitialized() ) {
                systems.initialize();
            }
            if( !systems.isStarted() ) {
                systems.start();
            }
            
            long lastTime = System.nanoTime();
            while( go.get() ) {
                long time = System.nanoTime();
                long delta = time - lastTime; 
                if( delta < updateRate ) {
                    // Not time to update yet
                    continue;
                }
                lastTime = time;                                        
                systems.update();
                
                // Wait just a little.  This is an important enough thread
                // that we'll poll instead of smart-sleep. 
                try {
                    Thread.sleep(0);
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

