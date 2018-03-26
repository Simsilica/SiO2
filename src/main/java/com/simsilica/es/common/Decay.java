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

package com.simsilica.es.common;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.PersistentComponent;

/**
 *  A standard component for tracking the life/death of an entity.
 *  It can also provide the percentage of time remaining from when
 *  it was first created.
 *
 *  @author    Paul Speed
 */
public class Decay implements EntityComponent, PersistentComponent {
    private long startTime;
    private long endTime;
    
    public Decay() {
    }
 
    /**
     *  Creates a decay component with a start/end time that can
     *  provide things like time remaining and percent remaining.
     */
    public Decay( long startTime, long endTime ) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public static Decay duration( long startTime, long duration ) {
        return new Decay(startTime, startTime + duration);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getTimeRemaining( long time ) {
        return Math.max(0, endTime - time);
    }
    
    public boolean isDead( long time ) {
        return time >= endTime; 
    }
    
    public double getPercentRemaining( long time ) {
        if( time >= endTime ) {
            return 0;
        }
        long remain = endTime - time;        
        double total = endTime - startTime;
        return remain / total; 
    }
 
    @Override   
    public String toString() {
        return "Decay[startTime=" + startTime + ", endTime=" + endTime + "]";
    } 
}
