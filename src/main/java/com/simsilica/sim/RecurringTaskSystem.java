/*
 * $Id$
 * 
 * Copyright (c) 2022, Simsilica, LLC
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

import java.util.*;
import java.util.function.Function;

import org.slf4j.*;

/**
 *  A GameSystem that supports recurring tasks.
 *
 *  @author    Paul Speed
 */
public class RecurringTaskSystem extends AbstractGameSystem {
 
    static Logger log = LoggerFactory.getLogger(RecurringTaskSystem.class);
 
    private List<Function<SimTime, Boolean>> tasks = new ArrayList<>();
    private volatile Function[] taskArray;
    private static Function[] EMPTY_ARRAY = new Function[0]; 
    
    public RecurringTaskSystem() {
    }
 
    /**
     *  Adds a recurring task that will continue to run once a frame
     *  as long as it returns Boolean.TRUE.
     */   
    public void addRecurringTask( Function<SimTime, Boolean> task ) {
        synchronized(tasks) {
            tasks.add(task);
            taskArray = null;
        }
    }
    
    public void removeRecurringTask( Function<SimTime, Boolean> task ) {
        synchronized(tasks) {
            tasks.remove(task);
            taskArray = null;
        }
    }

    protected Function[] getArray() {
        // Grab the array, unprotected, so it doesn't move underneath us
        Function[] result = taskArray;
        if( result != null ) {
            return result;
        }
        
        // Else a little double-checked locking
        synchronized(tasks) {
            // See if one was created in the mean time
            if( taskArray != null ) {
                return taskArray;
            }
            taskArray = tasks.toArray(EMPTY_ARRAY);
            return taskArray;
        }        
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update( SimTime time ) {        
        for( Function f : getArray() ) {
            Object result = f.apply(time);
            if( !Objects.equals(result, Boolean.TRUE) ) {
                removeRecurringTask((Function<SimTime, Boolean>)f);
            }
        }
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void terminate() {
    }
}


