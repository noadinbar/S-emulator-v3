package remote;

import api.ExecutionAPI;
import client.requests.Execute;
import client.responses.ExecuteResponder;
import client.responses.StatusResponder;
import com.google.gson.JsonObject;
import execution.ExecutionDTO;
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
            Request httpReq = Execute.build(request, userString);
            return ExecuteResponder.execute(httpReq);
        } catch (Exception e) {
            throw new RuntimeException("Execute failed: " + e.getMessage(), e);
        }
    }
}
