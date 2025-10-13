package client.responses;

import com.google.gson.JsonObject;
import execution.debug.DebugStepDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

public final class DebugResponder {

    private DebugResponder() {}

    /** קריאת init — מחזיר DebugResults.Init */
    public static DebugResults.Init init(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException("DEBUG init failed: HTTP " + res.code() + " | " + body);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);

            String debugId = (obj.has("debugId") && !obj.get("debugId").isJsonNull())
                    ? obj.get("debugId").getAsString()
                    : null;

            var stateEl = obj.get("state");
            var state = (stateEl != null && !stateEl.isJsonNull())
                    ? JsonUtils.GSON.fromJson(stateEl, execution.debug.DebugStateDTO.class)
                    : null;

            return new DebugResults.Init(debugId, state);
        }
    }

    /** קריאת step — מחזיר DebugStepDTO כמו שהוא */
    public static DebugStepDTO step(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException("DEBUG step failed: HTTP " + res.code() + " | " + body);
            }
            return JsonUtils.GSON.fromJson(body, DebugStepDTO.class);
        }
    }

    // כשנוסיף endpoins ל-stop/terminate, נוסיף כאן:
    // public static DebugResults.Stop stop(Request req) { ... }
    // public static DebugResults.Terminated terminated(Request req) { ... }
}
