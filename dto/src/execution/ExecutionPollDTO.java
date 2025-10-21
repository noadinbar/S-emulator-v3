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

    public ExecutionPollDTO(Status status, ExecutionDTO result, String error) {
        this.status = status;
        this.result = result;
        this.error = error;
    }

    public Status getStatus() { return status; }
    public ExecutionDTO getResult() { return result; }
    public String getError() { return error; }
}
