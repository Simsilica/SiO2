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

import com.google.common.base.MoreObjects;
import com.simsilica.event.EventType;


/**
 *  Event sent through the event bus for various GameSystemManager
 *  events.
 *
 *  @author    Paul Speed
 */
public class SimEvent {

    /**
     *  Event type signaling that the game system manager is initializing.
     */
    public static EventType<SimEvent> simInitializing = EventType.create("SimInitializing", SimEvent.class);

    /**
     *  Event type signaling that the game system manager has been initialized.
     */
    public static EventType<SimEvent> simInitialized = EventType.create("SimInitialized", SimEvent.class);

    /**
     *  Event type signaling that the game system manager is starting.
     */
    public static EventType<SimEvent> simStarting = EventType.create("SimStarting", SimEvent.class);

    /**
     *  Event type signaling that the game system manager has been started.
     */
    public static EventType<SimEvent> simStarted = EventType.create("SimStarted", SimEvent.class);

    /**
     *  Event type signaling that the game system manager is stopping.
     */
    public static EventType<SimEvent> simStopping = EventType.create("SimStopping", SimEvent.class);

    /**
     *  Event type signaling that the game system manager has been stopped.
     */
    public static EventType<SimEvent> simStopped = EventType.create("SimStopped", SimEvent.class);

    /**
     *  Event type signaling that the game system manager is terminating.
     */
    public static EventType<SimEvent> simTerminating = EventType.create("SimTerminating", SimEvent.class);

    /**
     *  Event type signaling that the game system manager has been terminated.
     */
    public static EventType<SimEvent> simTerminated = EventType.create("SimTerminated", SimEvent.class);

    /**
     *  Event type signaling that initialization or startup has failed in some way.
     */
    public static EventType<SimEvent> simFailed = EventType.create("SimFailed", SimEvent.class);

    private final GameSystemManager manager;

    public SimEvent( GameSystemManager manager ) {
        this.manager = manager;
    }

    public GameSystemManager getManager() {
        return manager;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .omitNullValues()
                .add("manager", manager)
                .toString();
    }
}


