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

    private int poolSize;
    private ThreadPoolExecutor workers; 
    private ConcurrentLinkedQueue<JobRunner> toFinish = new ConcurrentLinkedQueue<>();
    private int finishPerFrame = 1;
    
    private ConcurrentHashMap<Job, Job> queuedJobs = new ConcurrentHashMap<>();
    
    // An imperfect way of keeping track of the runners for a particular
    // job... but ok with the way it's used here.
    private ConcurrentHashMap<Job, JobRunner> runnerIndex = new ConcurrentHashMap<>();   
    
    private AtomicLong jobSequence = new AtomicLong(0); 

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
        this.poolSize = poolSize;
        this.finishPerFrame = finishPerFrame;
        
        // Need to do it manually if we want to give our own queue implementation.
        this.workers = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                                              new PriorityBlockingQueue<Runnable>());
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
        execute(job, DEFAULT_PRIORITY);
    }

    /**
     *  Queues the job for execution on a background thread using the
     *  specified priority.  Jobs with a lower priority value are executed
     *  first.
     */
    public void execute( Job job, int priority ) {
        // Right now we don't support changes in priority
        Job old = queuedJobs.putIfAbsent(job, job);
        if( old == null ) {
            if( log.isTraceEnabled() ) {
                log.trace("Queuing:" + job + "  at:" + priority);
            }
            // It's a new job
            JobRunner runner = new JobRunner(job, priority); 
            runnerIndex.put(job, runner); 
            workers.execute(runner);
        }
    }

    /**
     *  Cancels a job that is still waiting in the queue.  Returns
     *  true if the job was canceled, false if not.  It is not
     *  possible to cancel a job that is already being handled by
     *  a thread.
     */
    public boolean cancel( Job job ) {
        // Note that there is a slight but innocuous race condition here
        // in that we may be able to find a JobRunner in the index that
        // is not in the queue anymore when we try to remove it.  This
        // is ok, though.  It means the thread picked up the job between
        // when we grabbed the runner and when we tried to remove it 
        // from the queue so we won't be able to cancel it anyway.
        JobRunner runner = runnerIndex.get(job);
        if( runner == null ) {
            return false;
        }
        if( log.isTraceEnabled() ) {
            log.trace("Attempting to cancel:" + job);
        }
        if( workers.getQueue().remove(runner) ) {
            // Then cleanup the book-keeping, too
            queuedJobs.remove(job);
            runnerIndex.remove(job);
            
            // Note: the above assumes that the thread canceling the job
            // is the same one that might call execute() else the race 
            // condition mentioned above could mean that we remove a
            // just-added job.  Someday a write lock is probably warranted 
            // at least for the book-keeping updates.
            return true;
        }
        return false;
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
        return queuedJobs.containsKey(job);
    }

    /**
     *  Returns the current count of jobs waiting to be run.
     */
    public int getQueuedCountReference() {
        return queuedCount.getObject();
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
    public int getActiveCountReference() {
        return activeCount.getObject();
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
        workers.shutdownNow();
    }
    
    @Override
    protected void onEnable() {
    }
    
    @Override
    protected void onDisable() {
    }
    
    @Override
    public void update( float tpf ) {
        
        // Run a job cleanup until one indicates that it actually did something
        // Some jobs have no real work to do and it's unfair to delay other jobs just
        // because of that.
        JobRunner job = null;
        int count = 0;
        while( (job = toFinish.poll()) != null ) {
            if( log.isTraceEnabled() ) {
                log.trace("Finishing job:" + job.job + " at priority:" + job.priority);
            }
            if( job.job.runOnUpdate() ) {
                count++;
                if( finishPerFrame >= 0 && count >= finishPerFrame ) {
                    break;
                }
            }
        }
        
        queuedCount.updateObject(queuedJobs.size());
        activeCount.updateObject(workers.getActiveCount());        
    }
       
    
    private class JobRunner implements Runnable, Comparable<JobRunner> {
        private Job job;
        private int priority;
        
        // Keep a job ID just to make sure we can always sort
        // jobs even if their priority is the same.  Earlier job
        // wins in that case. 
        private long jobId = jobSequence.getAndIncrement();
        
        public JobRunner( Job job, int priority ) {
            this.job = job;
            this.priority = priority;
        }
        
        public int compareTo( JobRunner other ) {
            if( priority < other.priority ) {
                return -1;
            } else if( priority > other.priority ) {
                return 1;
            }
            if( jobId < other.jobId ) {
                return -1;
            } else if( jobId > other.jobId ) {
                return 1;
            }
            return 0;
        }
        
        public void run() {
            
            // We're running now so remove from the active set
            queuedJobs.remove(job);
            runnerIndex.remove(job);
        
            if( log.isTraceEnabled() ) {
                log.trace("Running background job:" + job + " at priority:" + priority);
            }
            try {
                job.runOnWorker();
            } catch( Exception e ) {
                log.error("Error running job:" + job, e);
                return;
            }
            toFinish.add(this);
        }
    }
}
