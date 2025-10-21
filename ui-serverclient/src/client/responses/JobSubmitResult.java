package client.responses;

public class JobSubmitResult {
    private final boolean accepted;
    private final String jobId;
    private final int retryMs; // אפשר לקרוא גם retryAfterMs אם את מעדיפה

    public static JobSubmitResult accepted(String jobId) { return new JobSubmitResult(true, jobId, 0); }
    public static JobSubmitResult busy(int retryMs)      { return new JobSubmitResult(false, null, retryMs); }

    private JobSubmitResult(boolean accepted, String jobId, int retryMs) {
        this.accepted = accepted; this.jobId = jobId; this.retryMs = retryMs;
    }
    public boolean isAccepted() { return accepted; }
    public String getJobId()    { return jobId; }
    public int getRetryMs()     { return retryMs; }
}
