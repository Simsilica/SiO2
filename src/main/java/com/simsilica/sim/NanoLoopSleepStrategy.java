/*
 * $Id$
 *
 * Copyright (c) 2020, Simsilica, LLC
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

import java.util.concurrent.locks.LockSupport;

/**
 *  LoopSleepStrategy that uses LockSupport.parkNanos().  This is not
 *  recommend to use on Windows because parkNanos() often has a minimum
 *  resolution of 16 ms or more on Windows 10.  It should be ok for
 *  Linux/etc boxen where LegacyLoopSleepStrategy is pegging a CPU at 100%.
 *  For more background, see: https://dzone.com/articles/locksupportparknanos-under-the-hood-and-the-curiou
 *
 *  Note: that on my Windows 7 box, it seems to make no difference which strategy
 *  that I pick.  -pspeed:2020/01/25
 *
 *  @author    Paul Speed
 */
public class NanoLoopSleepStrategy implements LoopSleepStrategy {

    private long updateRate;

    // Trying to predict the overhead latency in the nano-tracking system
    // to avoid sleep overruns where possible.  For accurate loop latency,
    // better to be too large than too small.
    // Default to 100 microseconds.
    private long expectedOverheadNanos = 100000;

    public NanoLoopSleepStrategy() {
    }

    public NanoLoopSleepStrategy( long updateRateNanos ) {
        this.updateRate = updateRateNanos;
    }

    /**
     *  Sets the desired update rate which can be used to calculate
     *  and execute an appropriate amount of sleep.
     */
    @Override
    public void setUpdateRateNanos( long updateRate ) {
        this.updateRate = updateRate;
    }

    @Override
    public long getUpdateRateNanos() {
        return updateRate;
    }

    /**
     *  Sets the expected overhead or minimum threshold for calls to
     *  LockSupport.parkNanos().  This time will be subtracted from sleep
     *  times.  The default is 100 microseconds or 100,000 nano seconds, ie: 0.1 ms.
     *  This is as measured by some folks online and it is more accurate for this
     *  value to be too larger versus too small.
     */
    public void setExpectedOsOverheadNanos( long expectedOverheadNanos ) {
        this.expectedOverheadNanos = expectedOverheadNanos;
    }

    public long getExpectedOverheadNanos() {
        return expectedOverheadNanos;
    }

    @Override
    public void loopSleep( long currentFrameTime, long pollDelta, long lastUpdateTime, long systemUpdateLength ) throws InterruptedException {

        // When would we ideally like another frame to run?
        long nextFrameTime = lastUpdateTime + updateRate;

        // What is 'now'?
        long currentTime = currentFrameTime;
        if( lastUpdateTime == currentFrameTime ) {
            // Then we just did an update so we should include that time
            // in current time
            currentTime += systemUpdateLength;
        }

        // Calculate a desired sleep time as the difference between 'now'
        // and the next desired update time.  We subtract an expected overhead
        // amount to try and avoid sleeping too long if possible.
        long delta = (nextFrameTime - currentTime) - expectedOverheadNanos;
        if( delta > 0 ) {
            // Throw the dice...
            // parkNanos() may sleep for the length of time we ask + some expected
            // overhead rate... it may also not sleep at all... or sleep much longer.
            // We hope for the best, though.
            LockSupport.parkNanos(delta);
        } else {
            // Either there was no time left to sleep because of previous loop
            // iterations that slept shorter than necessary... or the update
            // thread is so busy that it can't keep up.  Either way, sleeping
            // any amount of time will only introduce more lag.
        }
    }

}

