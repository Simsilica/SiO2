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


/**
 *  Provides information about the current simulation time step,
 *  including frame number, game time, and tpf.
 *
 *  @author    Paul Speed
 */
public class SimTime {
    private long frame;
    private long time;
    private long baseTime; // so that time can be based on some 0 value
    private double tpf;
    private double invTimeScale = 1000000000.0; // nanos  
    private double timeScale = 1.0 / invTimeScale;
    
    public SimTime() {
    }
    
    public void update( long time ) {
        if( frame == 0 ) {
            baseTime = time;
        }
        time -= baseTime;        
        frame++;
        tpf = (time - this.time) * timeScale;
        this.time = time;
    }
 
    /**
     *  Returns the SimTime version of the specified timestamp that
     *  is compatible with what would normally be provided to update().
     *  SimTime.getTime() will provide 'frame locked' timestamps that will
     *  not update during a frame which is what the systems will want 99.99%
     *  of the time.  Rarely it is desirable to get a "real" timestamp that 
     *  is not locked to frames, usually as part of providing matching accurate
     *  timesource information to something else that is trying to synch.
     */   
    public long getUnlockedTime( long time ) {
        return time - baseTime;
    }
 
    public long toSimTime( double seconds ) {
        return (long)(seconds * invTimeScale);
    }
 
    public long getFutureTime( double seconds ) {
        return time + toSimTime(seconds); 
    }        
    
    public final double getTpf() {
        return tpf;
    }
    
    public final long getFrame() {
        return frame;
    }
    
    public final long getTime() {
        return time;
    }
    
    public final double getTimeInSeconds() {
        return time * timeScale;
    }
    
    public final double getTimeScale() {
        return timeScale;
    }
 
    public final long addMillis( long ms ) {
        return time + ms * 1000000L;
    }
 
    @Override   
    public String toString() {
        return getClass().getSimpleName() + "[tpf=" + getTpf() + ", seconds=" + getTimeInSeconds() + "]";
    }
}
