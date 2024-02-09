/*
 * $Id$
 *
 * Copyright (c) 2023, Simsilica, LLC
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

package com.simsilica.thread;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.*;

import org.slf4j.*;

/**
 *  Manages an IterationProcessor such that one invocation of this object's
 *  iterate() method will execute one onIterate() of the processor, but on
 *  a background thread.
 *
 *  Calling lifecycle:
 *  IterationProcessorThread thread = new IterationProcessorThread(processor);
 *  thread.start();
 *  thread.iterate();
 *  thread.iterate();
 *  thread.iterate();
 *  thread.close();
 *
 *  On the background thread, the processor will see:
 *  onStart()
 *  onIterate()
 *  onIterate()
 *  onIterate()
 *  onStop()
 *
 *  If matchLoops is true then there will be one onIterate() call for every iterate() call
 *  even if onIterate() takes more than a frame to process.  It's up to the
 *  onIterate() of IterationProcessor to gate things based on time if catching
 *  up is not required.  If matchLoops is false then the thread will 'catch up' each
 *  time by skipping extra onIterate() calls.
 *
 *  @author    Paul Speed
 */
public class IterationProcessorThread extends Thread {
    static Logger log = LoggerFactory.getLogger(IterationProcessorThread.class);

    private Semaphore loopHold = new Semaphore(0);  // at least one release must happen before processing
    private AtomicBoolean go = new AtomicBoolean(true);
    private IterationProcessor processor;
    private boolean matchLoops;

    public IterationProcessorThread( IterationProcessor processor, boolean matchLoops ) {
        this.processor = processor;
        this.matchLoops = matchLoops;
    }

    public IterationProcessorThread( String name, IterationProcessor processor, boolean matchLoops ) {
        super(name);
        this.processor = processor;
        this.matchLoops = matchLoops;
    }

    public boolean getMatchLoops() {
        return matchLoops;
    }

    public void iterate() {
        // Release to run again
        loopHold.release();
    }

    public void close() {
        go.set(false);
        // Let it iterate to die
        iterate();
        try {
            join();
        } catch( InterruptedException e ) {
            throw new RuntimeException("Interrupted while closing", e);
        }
    }

    public void run() {
        processor.onStart();
        try {
            while( go.get() ) {
                loopHold.acquire();
                if( !matchLoops ) {
                    loopHold.drainPermits();
                }
                if( !go.get() ) {
                    // We've been asked to stop
                    return;
                }
                processor.onIterate();
            }
        } catch( InterruptedException e ) {
            if( go.get() ) {
                throw new RuntimeException("Unexpected interruption", e);
            }
        } finally {
            processor.onStop();
        }
    }
}

