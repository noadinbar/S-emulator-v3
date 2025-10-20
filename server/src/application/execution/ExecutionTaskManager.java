package application.execution;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class ExecutionTaskManager {

    public enum Status { PENDING, RUNNING, DONE, ERROR, CANCELED }

    public static final class Job {
        public final String id;
        public volatile Status status = Status.PENDING;
        public volatile ExecutionDTO result;
        public volatile String error;
        private Future<?> future;
        Job(String id) { this.id = id; }
        void attach(Future<?> f) { this.future = f; }
        public void cancel() { if (future != null) future.cancel(true); status = Status.CANCELED; }
    }

    // בסגנון המרצה: ExecutorService קבוע, אפשר עם ThreadFactory ידידותי
    private static final int POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());
    // אם תרצי Queue חסום עם back-pressure, השתמשי ב- ThreadPoolExecutor + LinkedBlockingQueue(capacity)
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(POOL_SIZE, r -> {
        Thread t = new Thread(r, "execute-worker");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, Job> JOBS = new ConcurrentHashMap<>();

    private ExecutionTaskManager() {}

    public static Job submit(ExecutionRequestDTO req, Supplier<ExecutionDTO> task) {
        String id = UUID.randomUUID().toString();
        Job job = new Job(id);
        JOBS.put(id, job);
        Future<?> f = EXEC.submit(() -> {
            job.status = Status.RUNNING;
            try {
                job.result = task.get();
                job.status = Status.DONE;
            } catch (Throwable t) {
                job.error = t.getClass().getSimpleName() + ": " + t.getMessage();
                job.status = Status.ERROR;
            }
        });
        job.attach(f);
        return job;
    }

    public static Job get(String id) { return JOBS.get(id); }
}
