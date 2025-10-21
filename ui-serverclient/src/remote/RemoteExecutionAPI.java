package remote;

import api.ExecutionAPI;
import client.requests.Execute;
import client.responses.ExecuteResponder;
import client.responses.StatusResponder;
import com.google.gson.JsonObject;
import execution.ExecutionDTO;
import execution.ExecutionPollDTO;
import execution.ExecutionRequestDTO;
import okhttp3.Request;

public class RemoteExecutionAPI implements ExecutionAPI {
    private final String userString; // null = תוכנית ראשית

    public RemoteExecutionAPI() {
        this.userString = null;
    }
    public RemoteExecutionAPI(String functionKey) {
        this.userString = functionKey;
    }

    @Override
    public int getMaxDegree() {
        try {
            JsonObject js = StatusResponder.get();
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
            // build original POST to get the exact URL
            Request submitReq = Execute.build(request, userString);
            String executeUrl = submitReq.url().toString();

            // submit -> jobId
            String jobId = ExecuteResponder.submit(
                    ExecuteResponder.buildSubmitRequest(executeUrl, request, userString)
            );

            // poll until terminal
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
