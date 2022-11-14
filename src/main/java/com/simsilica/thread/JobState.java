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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.slf4j.*;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;

/**
 *
 *
 *  @author    Paul Speed
 */
public class JobState extends BaseAppState {

    static Logger log = LoggerFactory.getLogger(JobState.class);

    public static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    private int finishPerFrame = 1;

    private WorkerPool workers;

    // Some stats.  These are only updated on the render thread.
    private VersionedHolder<Integer> queuedCount = new VersionedHolder<>(0);
    private VersionedHolder<Integer> activeCount = new VersionedHolder<>(0);

    public JobState() {
        this(4);
    }

    public JobState( int poolSize ) {
        this(null, poolSize, 1);
    }

    /**
     *  Creates a new JobState with the specified ID, worker poolSize,
     *  and finishPerFrame count.  If finishPerFrame is -1 then all
     *  finishable jobs will be finished each frame.
     */
    public JobState( String id, int poolSize, int finishPerFrame ) {
        super(id);
        this.finishPerFrame = finishPerFrame;

        workers = new WorkerPool(poolSize);
    }

    /**
     *  Sets the number of completed jobs to finish in a single render
     *  frame.  Defaults to 1.  Jobs that return false from their runOnUpdate()
     *  method do not count.  If this value is set to -1 then all jobs completed
     *  jobs are 'finished'.
     */
    public void setFinishPerFrame( int finishPerFrame ) {
        this.finishPerFrame = finishPerFrame;
    }

    public int getFinishPerFrame() {
        return finishPerFrame;
    }

    /**
     *  Queues the job for execution on a background thread using the
     *  default priority.  Jobs with a lower priority value are executed
     *  first.
     */
    public void execute( Job job ) {
        workers.execute(job);
    }

    /**
     *  Queues the job for execution on a background thread using the
     *  specified priority.  Jobs with a lower priority value are executed
     *  first.
     */
    public void execute( Job job, int priority ) {
        workers.execute(job, priority);
    }

    /**
     *  Cancels a job that is still waiting in the queue.  Returns
     *  true if the job was canceled, false if not.  It is not
     *  possible to cancel a job that is already being handled by
     *  a thread.
     */
    public boolean cancel( Job job ) {
        return workers.cancel(job);
    }

    /**
     *  Returns true if the job is already queued.  Note that this returns
     *  false if the job is already being handled by a worker thread.
     *  isQueued() is useful if you know that the 'job' object needs to
     *  be run again but want to avoid needlessly running it twice.
     *  If the job is already running then it is likely that it still
     *  needs to be run again to get the latest updates or whatever.
     */
    public boolean isQueued( Job job ) {
        return workers.isQueued(job);
    }

    /**
     *  Returns the pool size that was set for the worker pool.
     */
    public int getPoolSize() {
        return workers.getPoolSize();
    }

    /**
     *  Returns true if the worker pool has any pending work to do
     *  or is in the middle of doing that work.
     */
    public boolean isBusy() {
        return workers.isBusy();
    }

    /**
     *  Returns the current count of jobs waiting to be run.
     */
    public int getQueuedCount() {
        //return queuedCount.getObject();
        // The holder could be as much as a full frame delayed
        // so we'll query the workers directly.
        return workers.getQueuedJobCount();
    }

    /**
     *  Returns a VersionedReference for the current count of jobs
     *  waiting to be run.
     */
    public VersionedReference<Integer> createQueuedCountReference() {
        return queuedCount.createReference();
    }

    /**
     *  Returns the current count of jobs actually being handled by
     *  a thread or waiting to be 'finished'.
     */
    public int getActiveCount() {
        //return activeCount.getObject();
        // The holder could be as much as a full frame delayed
        // so we'll query the workers directly.
        return workers.getActiveJobCount();
    }

    /**
     *  Returns a VersionedReference for the current count of jobs
     *  actually being handled by a thread or waiting to be 'finished'.
     */
    public VersionedReference<Integer> createActiveCountReference() {
        return activeCount.createReference();
    }

    @Override
    protected void initialize( Application app ) {
    }

    @Override
    protected void cleanup( Application app ) {
        // Maintaining original functionality by passing 'false'.
        workers.shutdownNow(false);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update( float tpf ) {
        workers.update(finishPerFrame);
        queuedCount.updateObject(workers.getQueuedJobCount());
        activeCount.updateObject(workers.getActiveJobCount());
    }
}
