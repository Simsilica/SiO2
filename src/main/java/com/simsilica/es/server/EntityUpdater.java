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

package com.simsilica.es.server;

import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import com.simsilica.thread.IterationProcessorThread;
import com.simsilica.thread.IterationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  A game system that simply hooks into the game loop to
 *  periodically flush the entity buffers.  This is required
 *  for servers the use EntityHostedService.
 *
 *  @author    Paul Speed
 */
public class EntityUpdater extends AbstractGameSystem {
    static Logger log = LoggerFactory.getLogger(EntityUpdater.class);

    private final EntityDataHostedService entityService;
    private final IterationProcessorThread thread;
    private long checkThresholdNanos = 50 * 1000000L;

    public EntityUpdater( EntityDataHostedService entityService ) {
        this(entityService, false);
    }

    public EntityUpdater( EntityDataHostedService entityService, boolean backgroundThread ) {
        this.entityService = entityService;
        if( backgroundThread ) {
            this.thread = new IterationProcessorThread("EntityUpdater", new UpdateProcessor(), false);
        } else {
            this.thread = null;
        }
    }

    public void setTimingCheckThreshold( long checkMs ) {
        this.checkThresholdNanos = checkMs * 1000000L;
    }

    public long getTimingCheckThreshold() {
        return checkThresholdNanos / 1000000L;
    }

    @Override
    protected void initialize() {
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void update( SimTime time ) {
        if( thread != null ) {
            thread.iterate();
        } else {
            sendUpdates();
        }
    }

    @Override
    public void stop() {
        thread.close();
    }

    @Override
    protected void terminate() {
    }

    protected void sendUpdates() {
        long start = System.nanoTime();
        entityService.sendUpdates();
        long end = System.nanoTime();
        checkTiming(start, end);
    }

    protected void checkTiming( long start, long end ) {
        long delta = end - start;

        // If it takes longer than 50 ms then log a warning
        if( delta > checkThresholdNanos ) {
            log.warn(String.format("Entity updates exceed 50 ms: %.03f ms", delta/1000000.0));
        }
    }

    public class UpdateProcessor implements IterationProcessor {
        @Override
        public void onStart() {
        }

        @Override
        public void onIterate() {
            sendUpdates();
        }

        @Override
        public void onStop() {
        }
    }
}

