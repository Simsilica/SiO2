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

package com.simsilica.thread;


/**
 *  Implemented by objects that will perform some operation in
 *  the job state.  A job has a two part lifecycle where one 
 *  operation is run on a background thread managed by the JobState and
 *  then a second operation is run on the render thread once the
 *  job has completed.  This makes it easier to perform background
 *  operations that need to make modifications to the scene graph
 *  when complete.
 *
 *  @author    Paul Speed
 */
public interface Job {

    /**
     *  Called when the Job is run on the background thread.
     */
    public void runOnWorker();
    
    /**
     *  Called on the JME render thread when the background  processing 
     *  has completed.  Returns a rough indicator of the amount of work
     *  performed relative to the amount of work that is allowed per
     *  update of the scheduler.  For example, if "finishPerFrame" is 2
     *  then this would allow 2 jobs that return 1.0, or 20 jobs that return
     *  0.1... or any number of jobs that return 0.  It's a way to limit
     *  the amount of work done on update in a way that allows 'no-ops',
     *  'lots of updates', and 'barely any updates' to be dealt with
     *  intelligently. 
     */
    public double runOnUpdate();
}
