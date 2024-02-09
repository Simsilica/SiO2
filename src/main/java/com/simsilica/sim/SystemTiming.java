/*
 * $Id$
 *
 * Copyright (c) 2024, Simsilica, LLC
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

import org.slf4j.*;

/**
 *  Collects timing information about the running systems.
 *
 *  @author    Paul Speed
 */
public class SystemTiming {
    static Logger log = LoggerFactory.getLogger(SystemTiming.class);

    private final Map<GameSystem, TimingInfo> timingIndex = new LinkedHashMap<>();
    private long frameStart;
    private long frameEnd;
    private long timingCheckThresholdNanos = 100 * 1000000L;

    public SystemTiming() {
    }

    /**
     *  Sets the threshold in milliseconds that when exceeded will cause a log message to
     *  be written during endFrame().  Defaults to 100 ms.
     */
    public void setTimingCheckThreshold( long checkMs ) {
        this.timingCheckThresholdNanos = checkMs * 1000000L;
    }

    public long getTimingCheckThreshold() {
        return timingCheckThresholdNanos / 1000000L;
    }

    public void startFrame() {
        this.frameStart = System.nanoTime();
    }

    public void endFrame() {
        this.frameEnd = System.nanoTime();
        checkTiming(frameStart, frameEnd);
    }

    protected void checkTiming( long start, long end ) {
        long delta = end - start;
        if( delta > timingCheckThresholdNanos ) {
            StringBuilder sb = new StringBuilder();
            for( Map.Entry<GameSystem, TimingInfo> e : timingIndex.entrySet() ) {
                if( sb.length() > 0 ) {
                    sb.append(", ");
                }
                sb.append(e.getValue() + " : " + e.getKey());
            }
            log.warn(String.format("Update loop exceeds %d ms, at: %.03f ms  System info: %s",
                                   getTimingCheckThreshold(), delta/1000000.0, sb));
        }
    }

    public TimingInfo trackUpdate( GameSystem sys ) {
        return getTiming(sys, true).open();
    }

    public Map<GameSystem, TimingInfo> getAllTimingInfo() {
        return timingIndex;
    }

    protected TimingInfo getTiming( GameSystem sys, boolean create ) {
        TimingInfo result = timingIndex.get(sys);
        if( result == null && create ) {
            result = new TimingInfo(sys);
            timingIndex.put(sys, result);
        }
        return result;
    }

    public static class TimingInfo implements AutoCloseable {
        private final GameSystem sys;
        private long start;
        private long stop;

        public TimingInfo( GameSystem sys ) {
            this.sys = sys;
        }

        protected TimingInfo open() {
            this.start = System.nanoTime();
            return this;
        }

        public void close() {
            this.stop = System.nanoTime();
        }

        public long getDurationNanos() {
            return stop - start;
        }

        public double getDurationMillis() {
            return getDurationNanos() / 1000000.0;
        }

        @Override
        public String toString() {
            return String.format("%.03f ms", getDurationMillis());
        }
    }
}
