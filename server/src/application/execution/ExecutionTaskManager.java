package application.execution;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Manages async execution jobs with bounded queue and admission control.
 * Use trySubmit(...) for back-pressure (returns BUSY without throwing).
 */
public final class ExecutionTaskManager {

    public enum Status { PENDING, RUNNING, DONE, ERROR, CANCELED, TIMED_OUT }

    /** A single execution job state stored in registry. */
    public static final class Job {
        public final String id;
        public volatile Status status = Status.PENDING;
        public volatile Object result;
        public volatile String error;
        private volatile Future<?> future; // attached running task
        public final long createdAt = System.currentTimeMillis();
        private volatile long finishedAt = 0L;
        private volatile ScheduledFuture<?> timeoutHandle;

        public Job(String id) { this.id = id; }

        public void attach(Future<?> f) { this.future = f; }

        public void timeout() {
            status = Status.TIMED_OUT;
            finishedAt = System.currentTimeMillis();
        }

        public synchronized void cancel() {
            if (future != null) future.cancel(true);
            status = Status.CANCELED;
            finishedAt = System.currentTimeMillis();
            if (timeoutHandle != null) timeoutHandle.cancel(false);
        }
    }
    private static final long TIMEOUT_MS = 60_000; // 1 minute
    private static final long TTL_MS     = 15 * 60_000; // 15 minutes
    private static final ScheduledExecutorService SCHED =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "exec-timeouts");
                t.setDaemon(true);
                return t;
            });
    // Number of workers (parallel executions)
    private static final int N_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());
    // Max queued jobs waiting in the executor queue
    private static final int QUEUE_CAPACITY = 256;
    // Admission gate: total in-system = running + queued <= permits
    private static final Semaphore ADMISSION = new Semaphore(N_THREADS + QUEUE_CAPACITY);
    // Named worker threads for easier debugging
    private static final ThreadFactory EXEC_THREAD_FACTORY = new ThreadFactory() {
        private long idx = 0L;
        @Override public synchronized Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("execute-worker-" + (++idx));
            t.setDaemon(false);
            return t;
        }
    };
    // Bounded queue + AbortPolicy (never run heavy work on the caller / servlet thread)
    private static final ThreadPoolExecutor EXEC = new ThreadPoolExecutor(
            N_THREADS, N_THREADS,
            0L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            EXEC_THREAD_FACTORY,
            new ThreadPoolExecutor.AbortPolicy()
    );
    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();

    private ExecutionTaskManager() {}

    static {
        SCHED.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            JOBS.entrySet().removeIf(e -> {
                Job j = e.getValue();
                boolean terminal = j.status == Status.DONE
                        || j.status == Status.ERROR
                        || j.status == Status.CANCELED
                        || j.status == Status.TIMED_OUT;
                return terminal && j.finishedAt > 0 && (now - j.finishedAt) > TTL_MS;
            });
        }, 60, 60, TimeUnit.SECONDS);
    }

    // ---------- New API: non-throwing submit with BUSY response ----------

    /**
     * Submits a job if capacity allows. Returns ACCEPTED(jobId) or BUSY(retryAfterMs).
     * The supplied task should do the heavy execute() work (no repository locks here).
     */
    public static JobSubmitResult trySubmit(Supplier<ExecutionDTO> task) {
        // 1) Admission control: if system is full, respond BUSY immediately (no blocking, no exceptions)
        if (!ADMISSION.tryAcquire()) {
            return JobSubmitResult.busy(1500); // suggest UI to retry in ~1.5s
        }

        final String id = UUID.randomUUID().toString();
        final Job job = new Job(id);
        JOBS.put(id, job);

        boolean submitted = false;
        try {
            Future<?> f = EXEC.submit(() -> {
                job.status = Status.RUNNING;
                try {
                    ExecutionDTO dto = task.get(); // heavy compute
                    if (job.status != Status.CANCELED) {
                        job.result = dto;
                        job.status = Status.DONE;
                    }
                } catch (Throwable t) {
                    boolean canceled = Thread.currentThread().isInterrupted()
                            || (t instanceof CancellationException);
                    if (canceled) {
                        job.status = Status.CANCELED;
                    } else if (job.status != Status.CANCELED) {
                        job.error = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
                        job.status = Status.ERROR;
                    }
                } finally {
                    job.finishedAt = System.currentTimeMillis();
                    if (job.timeoutHandle != null) job.timeoutHandle.cancel(false);
                    // Always release system capacity when job is finished for any reason
                    ADMISSION.release();
                }
            });
            job.attach(f);
            // קובע timeout אוטומטי: אם לא הסתיים בזמן → מסמן TIMED_OUT ומבצע interrupt
            job.timeoutHandle = SCHED.schedule(() -> {
                // נוודא שלא כבר הסתיים
                if (job.status == Status.PENDING || job.status == Status.RUNNING) {
                    job.timeout();                 // status=TIMED_OUT + finishedAt
                    Future<?> ff = job.future;
                    if (ff != null) ff.cancel(true); // interrupt לריצה
                }
                }, TIMEOUT_MS, TimeUnit.MILLISECONDS);
            submitted = true;
            return JobSubmitResult.accepted(job.id);

        } catch (RejectedExecutionException rex) {
            // (Double safety) executor queue overflow — translate to BUSY
            JOBS.remove(id);
            return JobSubmitResult.busy(1500);

        } finally {
            if (!submitted) {
                // If submission failed after acquire, free the slot
                ADMISSION.release();
            }
        }
    }

    // ---------- Legacy API (kept for compatibility) ----------

    /**
     * Legacy: submit and always accept (no BUSY). Prefer trySubmit(...) in new code.
     * Left intact for compatibility with existing call sites.
     */
    public static Job submit(ExecutionRequestDTO req, Supplier<ExecutionDTO> task) {
        String id = UUID.randomUUID().toString();
        Job job = new Job(id);
        JOBS.put(id, job);

        Future<?> f = EXEC.submit(() -> {
            job.status = Status.RUNNING;
            try {
                ExecutionDTO dto = task.get();    // may throw if canceled
                if (job.status != Status.CANCELED) {
                    job.result = dto;
                    job.status = Status.DONE;
                }
            } catch (Throwable t) {
                boolean canceled = Thread.currentThread().isInterrupted()
                        || (t.getMessage() != null && t.getMessage().equals("Canceled"))
                        || (t instanceof CancellationException);
                if (canceled) {
                    job.status = Status.CANCELED;
                } else if (job.status != Status.CANCELED) {
                    job.error = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
                    job.status = Status.ERROR;
                }
            }
        });
        job.attach(f);
        return job;
    }

    // ---------- Accessors / metrics ----------

    public static Job get(String id) { return JOBS.get(id); }

    public static int activeCount()   { return EXEC.getActiveCount(); }
    public static int queueSize()     { return EXEC.getQueue().size(); }
    public static int queueCapacity() { return QUEUE_CAPACITY; }

    // ---------- Lifecycle ----------

    public static void shutdown() {
        EXEC.shutdown(); // graceful
        try {
            if (!EXEC.awaitTermination(15, TimeUnit.SECONDS)) {
                EXEC.shutdownNow(); // force if needed
            }
        } catch (InterruptedException ie) {
            EXEC.shutdownNow();
            Thread.currentThread().interrupt();
        }
        JOBS.clear(); // best-effort cleanup
        SCHED.shutdownNow();
    }
}
