package client.responses;

import com.google.gson.JsonObject;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

public final class DebugResponder {

    private DebugResponder() {}

    /** init → מחזיר DebugResults.Init */
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

    /** step → מחזיר DebugStepDTO כמו שהוא */
    public static DebugStepDTO step(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException("DEBUG step failed: HTTP " + res.code() + " | " + body);
            }
            return JsonUtils.GSON.fromJson(body, DebugStepDTO.class);
        }
    }

    /** stop → מחזיר DebugResults.Stop (expect: { "stopped": true, "debugId": "..." }) */
    public static DebugResults.Stop stop(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException("DEBUG stop failed: HTTP " + res.code() + " | " + body);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            boolean stopped = obj.has("stopped") && obj.get("stopped").getAsBoolean();
            String debugId  = (obj.has("debugId") && !obj.get("debugId").isJsonNull())
                    ? obj.get("debugId").getAsString()
                    : null;
            return new DebugResults.Stop(stopped, debugId);
        }
    }

    public static DebugResults.Terminated terminated(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                return new DebugResults.Terminated(false);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            boolean term = obj.has("terminated") && obj.get("terminated").getAsBoolean();
            return new DebugResults.Terminated(term);
        }
    }

    public static DebugResults.Resume resume(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                return new DebugResults.Resume(false, 0, null, null);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            boolean terminated = obj.has("terminated") && obj.get("terminated").getAsBoolean();
            int steps = obj.has("steps") ? obj.get("steps").getAsInt() : 0;
            DebugStateDTO last = (obj.has("lastState") && !obj.get("lastState").isJsonNull())
                    ? JsonUtils.GSON.fromJson(obj.get("lastState"), DebugStateDTO.class)
                    : null;
            String debugId = (obj.has("debugId") && !obj.get("debugId").isJsonNull())
                    ? obj.get("debugId").getAsString() : null;
            return new DebugResults.Resume(terminated, steps, last, debugId);
        }
    }
}
