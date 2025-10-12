package remote;

import api.ExecutionAPI;
import client.responses.ExecuteResponder;
import client.responses.StatusResponder;
import com.google.gson.JsonObject;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import okhttp3.Request;

public class RemoteExecutionAPI implements ExecutionAPI {
    private final String userString;
    private final int degree;       // לעתיד (execute)

    public RemoteExecutionAPI() {
        this.userString = null;
        this.degree = 0;
    }
    public RemoteExecutionAPI(int degree) {
        this.userString = null;
        this.degree = degree;
    }
    public RemoteExecutionAPI(String functionKey) {
        this.userString = functionKey;
        this.degree = 0;
    }
    public RemoteExecutionAPI(String functionKey, int degree) {
        this.userString = functionKey;
        this.degree = degree;
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
        } catch (Exception ignore) { }
        return 0;
    }

    @Override
    public ExecutionDTO execute(ExecutionRequestDTO request) {
        try {
            Request httpReq = client.requests.Execute.build(request, userString);
            return ExecuteResponder.execute(httpReq);
        } catch (Exception e) {
            throw new RuntimeException("Execute failed: " + e.getMessage(), e);
        }
    }
}
