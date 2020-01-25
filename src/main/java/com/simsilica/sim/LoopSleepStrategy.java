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
 *  Implemented to provide the loop sleeping strategy for the GameLoop.
 *  A side-effect of this is that applications can also intercept
 *  these calls to keep accuracy statistics if they so choose.
 *
 *  @author    Paul Speed
 */
public interface LoopSleepStrategy {

    /**
     *  Called by the game loop to sleep in an appropriate way to maintain
     *  polling accuracy.  currentFrameTime and pollDelta represent information
     *  about the current iteration through the game loop. lastFrameTime and
     *  systemUpdateLength represent information about the last time that systems.update()
     *  was run... which may be less often than the game loop runs.
     *
     *  @param currentFrameTime is the actual system clock nano time at the beginning of this frame.
     *  @param pollDelta is the nanos since the last time the update loop ran.
     *  @param lastFrameTime is the system clock nano time the last time that systems.update() was run.
     *  @param systemUpdateLength is the actual nanoseconds that systems.update() took to perform the last time it
     *          was run.
     */
    public void loopSleep( long currentFrameTime, long pollDelta, long lastFrameTime, long systemUpdateLength ) throws InterruptedException;

    /**
     *  Sets the desired update rate which can be used to calculate
     *  and execute an appropriate amount of sleep.
     */
    public void setUpdateRateNanos( long updateRate );

    public long getUpdateRateNanos();
}


