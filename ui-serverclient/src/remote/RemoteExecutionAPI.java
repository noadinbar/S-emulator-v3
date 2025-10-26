package remote;

import api.ExecutionAPI;
import client.requests.runtime.Execute;
import client.responses.runtime.ExecuteResponder;
import client.responses.runtime.JobSubmitResult;
import client.responses.info.StatusResponder;
import com.google.gson.JsonObject;
import execution.ExecutionDTO;
import execution.ExecutionPollDTO;
import execution.ExecutionRequestDTO;
import okhttp3.Request;

public class RemoteExecutionAPI implements ExecutionAPI {
    private final String programName;
    private final String userString;

    public RemoteExecutionAPI(String programName) {
        this.programName = programName;
        this.userString = null;
    }

    public RemoteExecutionAPI(String programName, String functionKey) {
        this.programName = programName;
        this.userString = functionKey;
    }

    @Override
    public int getMaxDegree() {
        try {
            JsonObject js = StatusResponder.get(programName);
            if (js == null) return 0;

            if (userString == null || userString.isBlank()) {
                return (js.has("maxDegree") && !js.get("maxDegree").isJsonNull())
                        ? js.get("maxDegree").getAsInt()
                        : 0;
            }
            if (js.has("functionsMaxDegrees") && js.get("functionsMaxDegrees").isJsonObject()) {
                JsonObject fm = js.getAsJsonObject("functionsMaxDegrees");
                if (fm.has(userString) && !fm.get(userString).isJsonNull()) {
                    return fm.get(userString).getAsInt();
                }
            }
        } catch (Exception ignore) {}
        return 0;
    }

    @Override
    public ExecutionDTO execute(ExecutionRequestDTO request) {
        try {
            // Build once to get the exact URL (single source of truth)
            Request submitReq = Execute.build(request, programName, userString);
            String executeUrl = submitReq.url().toString();

            // ---- SUBMIT PHASE ----
            String jobId;
            while (true) {
                JobSubmitResult sr = ExecuteResponder.submit(
                        ExecuteResponder.buildSubmitRequest(executeUrl, request, programName, userString)
                );
                if (sr.isAccepted()) {
                    jobId = sr.getJobId();
                    break;
                }
                try { Thread.sleep(Math.max(300, sr.getRetryMs())); } catch (InterruptedException ignore) {}
            }

            // ---- POLL PHASE ----
            while (true) {
                Request pollReq = ExecuteResponder.buildPollRequest(executeUrl, jobId);
                ExecutionPollDTO pr = ExecuteResponder.poll(pollReq);
                switch (pr.getStatus()) {
                    case PENDING:
                    case RUNNING:
                        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
                        continue;
                    case DONE:
                        return pr.getResult();
                    case CANCELED:
                        throw new RuntimeException("Canceled");
                    case TIMED_OUT:
                    case ERROR:
                    default:
                        String err = (pr.getError() == null || pr.getError().isBlank())
                                ? "Unknown error"
                                : pr.getError();
                        throw new RuntimeException("Execute failed: " + err);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Execute failed: " + e.getMessage(), e);
        }
    }
}
