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


/**
 *  Reimplements the sleep behavior from before the LoopSleepStrategy
 *  refactoring was done.  Sleep is only called if the actual polling
 *  time is less than half of the desired polling time... and then the
 *  idleSleepTime value is used.  By default, this strategy will 'busy wait'
 *  if the time between polling frames is more than half the update rate.
 *  "Busy wait" in this case means that by default it will sleep(0) which
 *  should only give up the thread if there are other higher priority threads
 *  waiting.  This will make it look like the CPU (or a CPU) as at 100%
 *  even if it's not really impacting system performance.
 *  Increasing the idleSleepTime to 1 will cause the loop to always give
 *  up at least one time slice... making CPU look nice but often a time
 *  slice is too long to maintain accurate loop timing.
 *  This is especially true on Windows where there is no way to sleep
 *  for less than a millisecond and on newer windows versions, it's much
 *  worse.  For deeper background, see:
 *    https://dzone.com/articles/locksupportparknanos-under-the-hood-and-the-curiou
 *
 *  For what it's worth, on my 6 core Windows 7 box running with sleep(0), I never
 *  see any CPUs idle-pegging at an update rate of 60 hz.  So LegacyLoopSleepStrategy
 *  is probably a good suggestion for Windows-based machines.
 *
 *  @author    Paul Speed
 */
public class LegacyLoopSleepStrategy implements LoopSleepStrategy {

    private long updateRate;
    private long idleSleepTime = 0;

    public LegacyLoopSleepStrategy() {
    }

    public LegacyLoopSleepStrategy( long updateRateNanos ) {
        this.updateRate = updateRateNanos;
    }

    /**
     *  Sets the period of time in milliseconds that the update loop will
     *  wait between time interval checks.  This defaults to 0 because on
     *  Windows the default 60 FPS update rate will cause frame drops for
     *  a idle sleep time of 1 or more.  However, 0 causes the loop to consume
     *  a noticable amount of CPU.  For lower framerates, 1 is recommended.
     */
    public void setIdleSleepTime( long idleSleepTime ) {
        this.idleSleepTime = idleSleepTime;
    }

    public long getIdleSleepTime() {
        return idleSleepTime;
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

    @Override
    public void loopSleep( long currentFrameTime, long pollDelta, long lastUpdateTime, long systemUpdateLength ) throws InterruptedException {

        // Below are the comments from the original implementation.
        // -pspeed 2020/01/25

        // Wait just a little.  This is an important enough thread
        // that we'll poll instead of smart-sleep but we also don't
        // want to consume 100% of a CPU core.
        // Thread.sleep(0) relies on the operating systems thread
        // scheduler which will have a minimum granularity.  On Windows,
        // Thread.sleep(0) can take as long as 15 ms to return.  Thus
        // note that if we devolve into a case where the delta is more
        // than the update rate then we never hit this code.
        //
        // And if the delta since last poll was kind of large
        // we won't sleep at all... In this case, if the delta was
        // more than half the updateRate... we'll skip sleeping.
        // Which probably means that on Windows, we'll be responsive
        // every other loop iteration.  It's an imperfect world and
        // the only alternative is true CPU heating busy-waiting.
        if( pollDelta * 2 > updateRate ) {
            return;
        }

        // More then 0 here and we underflow constantly.
        // 0 and we gobble up noticable CPU.  Slower update
        // rates could probably get away with the longer sleep
        // but 60 FPS can't keep up on Windows.  So we'll let
        // the sleep time be externally defined for cases
        // where the caller knows  better.
        Thread.sleep(idleSleepTime);
    }

}

