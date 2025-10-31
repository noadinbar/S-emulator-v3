package execution;

/**
 * Poll response DTO for async execution.
 * status: PENDING / RUNNING / DONE / ERROR / CANCELED
 * result: present only when DONE
 * error:  present only when ERROR (stringified)
 */
public class ExecutionPollDTO {

    public enum Status { PENDING, RUNNING, DONE, ERROR, CANCELED, TIMED_OUT }

    private Status status;
    private ExecutionDTO result;
    private String error;
    private boolean outOfCredits;

    public ExecutionPollDTO(Status status, ExecutionDTO result, String error, boolean outOfCredits) {
        this.status = status;
        this.result = result;
        this.error = error;
        this.outOfCredits = outOfCredits;
    }

    public Status getStatus() { return status; }
    public ExecutionDTO getResult() { return result; }
    public String getError() { return error; }
    public boolean isOutOfCredits() {
        return outOfCredits;
    }
}
