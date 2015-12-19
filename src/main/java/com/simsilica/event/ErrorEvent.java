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

package com.simsilica.event;

import com.google.common.base.MoreObjects;


/**
 *  A general error event that can be used to signal application
 *  events across the event bus.  Optional information about the
 *  "original event" is included for event dispatch errors.
 *
 *  @author    Paul Speed
 */
public class ErrorEvent {

    /**
     *  Event type signaling an error during event dispatch.  Additional
     *  information about the originalType and originalEvent will be filled
     *  in on the passed ErrorEvent object.
     */ 
    public static EventType<ErrorEvent> dispatchError = EventType.create("DispatchError", ErrorEvent.class);
 
    /**
     *  A general fatal application error.  This indicates that the application
     *  should shut down immediately.
     */   
    public static EventType<ErrorEvent> fatalError = EventType.create("FatalError", ErrorEvent.class);
       
    private final Throwable error;
    private final EventType originalType;
    private final Object originalEvent;
    
    public ErrorEvent( Throwable error, EventType originalType, Object originalEvent ) {
        this.error = error;
        this.originalType = originalType;
        this.originalEvent = originalEvent;
    }
    
    public ErrorEvent( Throwable error ) {
        this(error, null, null);
    }
    
    public Throwable getError() {
        return error;
    }
    
    public EventType getOriginalType() {
        return originalType;
    }
    
    public Object getOriginalEvent() {
        return originalEvent;
    }
 
    @Override   
    public String toString() {
        return MoreObjects.toStringHelper(getClass().getSimpleName())
                .omitNullValues()
                .add("error", error)
                .add("originalType", originalType)
                .add("originalEvent", originalEvent)
                .toString();
    }
}


