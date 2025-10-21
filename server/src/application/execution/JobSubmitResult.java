package application.execution;

public class JobSubmitResult {
    private final boolean accepted;
    private final String jobId;
    private final int retryAfterMs;

    public static JobSubmitResult accepted(String jobId) { return new JobSubmitResult(true, jobId, 0); }
    public static JobSubmitResult busy(int retryAfterMs) { return new JobSubmitResult(false, null, retryAfterMs); }

    private JobSubmitResult(boolean accepted, String jobId, int retryAfterMs) {
        this.accepted = accepted; this.jobId = jobId; this.retryAfterMs = retryAfterMs;
    }
    public boolean isAccepted() { return accepted; }
    public String getJobId()    { return jobId; }
    public int getRetryAfterMs(){ return retryAfterMs; }
}
