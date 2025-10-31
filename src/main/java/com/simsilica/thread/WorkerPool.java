/*
 * $Id$
 *
 * Copyright (c) 2021, Simsilica, LLC
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

import com.google.common.base.Throwables;

/**
 *  Manages a thread pool that can be used to run Job objects that
 *  have a two phase execution: 1) run on a background thread, 2)
 *  run on an update thread once completed.
 *
 *  This class is suitable for wrapping in AppStates or GameSystems.
 *
 *  @author    Paul Speed
 */
public class WorkerPool {
    static Logger log = LoggerFactory.getLogger(WorkerPool.class);

    public static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    private int poolSize;
    private ThreadPoolExecutor workers;

    // It's possible that this should also be a priority queu to make
    // sure that objects get finished in priority order also.
    private ConcurrentLinkedQueue<JobRunner> toFinish = new ConcurrentLinkedQueue<>();

    private ConcurrentHashMap<Job, Job> queuedJobs = new ConcurrentHashMap<>();

    // An imperfect way of keeping track of the runners for a particular
    // job... but ok with the way it's used here.
    private ConcurrentHashMap<Job, JobRunner> runnerIndex = new ConcurrentHashMap<>();

    private AtomicLong jobSequence = new AtomicLong(0);

    // To avoid race conditions when adding working jobs to the jobs
    // waiting to be finished, we will track the count of every job start
    // to finish.  It's not a huge deal that this be 100% accurate but it
    // avoids temporarily showing extra jobs if we happen to check the workers
    // size right before it passed something to the toFinish queue.
    // workers.size() + toFinish.size() might temporarily show more jobs
    // than were ever submitted otherwise.
    private AtomicInteger activeCount = new AtomicInteger(0);

    private AtomicLong errorCount = new AtomicLong(0);

    private boolean shuttingDown = false;

    /**
     *  Creates a worker pool with 4 worker threads.
     */
    public WorkerPool() {
        this(4);
    }

    /**
     *  Creates a worker pool with the specified number of worker
     *  threads.
     */
    public WorkerPool( int poolSize ) {
        this.poolSize = poolSize;

        // Need to do it manually if we want to give our own queue implementation.
        this.workers = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                                              new PriorityBlockingQueue<Runnable>());
    }

    /**
     *  Returns the pool size that was set for this worker pool.
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     *  Queues the job for execution on a background thread using the
     *  default priority.  Jobs with a lower priority value are executed
     *  first.  (The default priority is Integer.MAX_VALUE making sure that
     *  any jobs with a real priority specified are run first.)
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
        } else {
            // We already have this job queued, what state is it in?
            JobRunner runner = runnerIndex.get(job);
            if( log.isTraceEnabled() ) {
                log.trace("existing:" + runner + "  new priority:" + priority);
            }
            // There is technically a possible race condition here where if
            // two different threads execute the same job (or cancel + execute, execute + cancel, etc.)
            // that might see the runner in the index but not in the queue.
            // I think this would only be a problem if a separate thread was trying to cancel
            // this job while we're executing it.  But I think 99% of the use cases, the same
            // thread will be executing the job as will be canceling it.
            if( runner.priority != priority ) {
                long start = System.nanoTime();
                if( workers.getQueue().remove(runner) ) {
                    if( log.isTraceEnabled() ) {
                        log.trace("Requeing:" + job + "  at:" + priority);
                    }
                    runner.priority = priority;
                    workers.execute(runner);
                }
                long end = System.nanoTime();
                if( log.isTraceEnabled() ) {
                    log.trace(String.format("requeue time: %.03f ms", (end - start) / 1000000.0));
                }
            }
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
            if( log.isTraceEnabled() ) {
                log.trace("Unknown job:" + job);
            }
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
            if( log.isTraceEnabled() ) {
                log.trace("Job canceled:" + job);
            }
            return true;
        }
        if( log.isTraceEnabled() ) {
            log.trace("Job no longer in queue:" + job);
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
     *  Returns the number of jobs waiting for a worker to pick them up.
     */
    public int getQueuedJobCount() {
        return queuedJobs.size();
    }

    /**
     *  Returns the number of jobs that are currently being handled
     *  by a thread or waiting to be 'finished'.
     */
    public int getActiveJobCount() {
        return activeCount.get();
    }

    /**
     *  Returns true if the worker pool has any pending work to do
     *  or is in the middle of doing that work.
     */
    public boolean isBusy() {
        return (getActiveJobCount() + getQueuedJobCount()) > 0;
    }

    /**
     *  Performs an immediate shutdown where all active threads are
     *  interrupted and any jobs queued are not executed.  Jobs waiting
     *  to finish can sitll be finished by calling the update()
     *  method.  Pass true for awaitTermination to wait until all threads
     *  have completed.
     */
    public void shutdownNow( boolean awaitTermination ) {
        shuttingDown = true;
        workers.shutdownNow();
        if( awaitTermination ) {
            log.info("Waiting for thread pool shutdown");
            try {
                // Essentially wait forever
                workers.awaitTermination(10000, TimeUnit.DAYS);
            } catch( InterruptedException e ) {
                throw new RuntimeException("Interrupted waiting for shutdown", e);
            }
        }
    }

    /**
     *  Returns true if the worker pool has not been shutdown.
     */
    public boolean isRunning() {
        return !workers.isShutdown();
    }

    /**
     *  Calls the runOnUpdate() method for any jobs that have been completed
     *  by workers up to the amount of work specified by maxWork.  Jobs
     *  return a "work amount" from their runOnUpdate() method that contributes
     *  to the total work value.  This allows jobs to give a hint as to how
     *  much processing their runOnUpdate() performs relative to other jobs.
     *  0 indicates no processing at all, larger values are application specific.
     *  At least one job will always be run if any jobs are pending.
     *  Specifying 0 for maxWork will run all jobs waiting for runOnUpdate() regardless
     *  of work size.
     *  Returns the amount of work actually performed.
     */
    public double update( double maxWork ) {
        JobRunner job = null;
        double totalWork = 0;
        while( (job = toFinish.poll()) != null ) {
            if( log.isTraceEnabled() ) {
                log.trace("Finishing job:" + job.job + " at priority:" + job.priority);
            }
            try {
                double work = job.job.runOnUpdate();
                if( log.isTraceEnabled() ) {
                    log.trace("job:" + job.job + "  work:" + work + "  totalWork:" + totalWork + "  maxWork:" + maxWork);
                }
                totalWork += work;
                if( maxWork >= 0 && totalWork >= maxWork ) {
                    // Any stragglers will be caught on the next pass
                    break;
                }
            } catch( RuntimeException e ) {
                errorCount.incrementAndGet();
                throw e;
            } finally {
                activeCount.decrementAndGet();
            }
        }
        return totalWork;
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
            activeCount.incrementAndGet();

            // We're running now so remove from the active set
            queuedJobs.remove(job);
            runnerIndex.remove(job);

            if( log.isTraceEnabled() ) {
                log.trace("Running background job:" + job + " at priority:" + priority);
            }
            try {
                job.runOnWorker();
            } catch( Exception e ) {
                if( shuttingDown && Throwables.getRootCause(e) instanceof InterruptedException ) {
                    log.info("Thread interrupted successfully");
                } else {
                    log.error("Error running job:" + job, e);
                    activeCount.decrementAndGet();
                    errorCount.incrementAndGet();
                }
                return;
            }
            if( log.isTraceEnabled() ) {
                log.trace("Job runOnWorker() done:" + job);
            }
            toFinish.add(this);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[jobId:" + jobId + ", priority:" + priority + ", job:" + job + "]";
        }
    }
}
