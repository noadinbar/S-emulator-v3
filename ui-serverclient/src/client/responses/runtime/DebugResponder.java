package client.responses.runtime;

import com.google.gson.JsonObject;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;
import utils.Constants;

import java.util.concurrent.TimeUnit;

public final class DebugResponder {

    private DebugResponder() {}

    // --------------------- Async semantics under the same names ---------------------

    /** POST /api/debug/init → 202 {debugId} | 429 {retryMs}
     *  Returns DebugResults.Submit (does NOT block for state).
     */
    public static DebugResults.InitResult init(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() == 202) {
                JsonObject root = JsonUtils.GSON.fromJson(body, JsonObject.class);
                String debugId = (root != null && root.has("debugId") && !root.get("debugId").isJsonNull())
                        ? root.get("debugId").getAsString()
                        : null;
                int creditsCurrent = 0;
                if (root != null && root.has("creditsCurrent") && !root.get("creditsCurrent").isJsonNull()) {
                    creditsCurrent = root.get("creditsCurrent").getAsInt();
                }
                return new DebugResults.InitResult(true, debugId, 0, false, creditsCurrent);
            }
            if (res.code() == 429) {
                JsonObject root = (body.isEmpty() ? null : JsonUtils.GSON.fromJson(body, JsonObject.class));
                int retryMs = (root != null && root.has("retryMs")) ? root.get("retryMs").getAsInt() : 1000;
                return new DebugResults.InitResult(false, null, retryMs, false, 0);
            }
            throw new RuntimeException("DEBUG init failed: HTTP " + res.code() + " | " + body);
        }
    }

    /** POST /api/debug/resume → 202 {debugId} | 429 {retryMs} | 409 (locked per-session)
     *  Returns DebugResults.Submit (async run started or advice to retry).
     */
    public static DebugResults.Submit resume(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            int code = res.code();

            if (code == 202) {
                JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
                String debugId = (obj != null && obj.has("debugId") && !obj.get("debugId").isJsonNull())
                        ? obj.get("debugId").getAsString() : null;
                return DebugResults.accepted(debugId);
            }
            if (code == Constants.SC_TOO_MANY_REQUESTS) {
                int retryMs = parseRetryMs(body, res);
                return DebugResults.busy(Math.max(300, retryMs));
            }
            if (code == 409) {
                return DebugResults.locked();
            }
            throw new RuntimeException("DEBUG resume (async) failed: HTTP " + code + " | " + body);
        }
    }

    /** GET /api/debug/state?debugId=... → 200 {state} | 204 no content | 404 unknown id
     *  Uses per-call timeout via HttpClientUtil.runSyncWithTimeout (keeps single OkHttpClient).
     */
    public static DebugStateDTO state(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSyncWithTimeout(req, 3, TimeUnit.SECONDS)) {
            String body = (res.body() != null) ? res.body().string() : "";
            int code = res.code();

            if (code == 200) {
                JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
                if (obj != null && obj.has("state") && !obj.get("state").isJsonNull()) {
                    return JsonUtils.GSON.fromJson(obj.get("state"), DebugStateDTO.class);
                }
                return null;
            }
            if (code == 204) return null; // session exists, snapshot not ready yet
            if (code == 404) return null; // unknown debugId (ended/cleared)
            throw new RuntimeException("DEBUG state failed: HTTP " + code + " | " + body);
        }
    }

    public static DebugResults.StepResult step(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException(
                        "DEBUG step failed: HTTP " + res.code() + " | " + body
                );
            }

            JsonObject root = JsonUtils.GSON.fromJson(body, JsonObject.class);
            if (root == null) {
                throw new RuntimeException("DEBUG step failed: empty JSON");
            }

            // the inner "step" object from server → real engine step dto
            DebugStepDTO stepDto = null;
            if (root.has("step") && !root.get("step").isJsonNull()) {
                stepDto = JsonUtils.GSON.fromJson(root.get("step"), DebugStepDTO.class);
            }

            // credits info (remaining + used)
            int creditsCurrent = 0;
            int creditsUsed = 0;
            if (root.has("credits") && root.get("credits").isJsonObject()) {
                JsonObject c = root.getAsJsonObject("credits");
                if (c.has("current") && !c.get("current").isJsonNull()) {
                    creditsCurrent = c.get("current").getAsInt();
                }
                if (c.has("used") && !c.get("used").isJsonNull()) {
                    creditsUsed = c.get("used").getAsInt();
                }
            }

            // did the server tell us this debug session is done?
            boolean terminated = false;
            if (root.has("terminated") && !root.get("terminated").isJsonNull()) {
                terminated = root.get("terminated").getAsBoolean();
            }

            return new DebugResults.StepResult(
                    stepDto,
                    creditsCurrent,
                    creditsUsed,
                    terminated
            );
        }
    }

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
                throw new RuntimeException(
                        "DEBUG terminated failed: HTTP " + res.code() + " | " + body
                );
            }
            JsonObject root = JsonUtils.GSON.fromJson(body, JsonObject.class);
            boolean term = false;
            int creditsCurrent = 0;
            if (root != null) {
                if (root.has("terminated") && !root.get("terminated").isJsonNull()) {
                    term = root.get("terminated").getAsBoolean();
                }
                if (root.has("creditsCurrent") && !root.get("creditsCurrent").isJsonNull()) {
                    creditsCurrent = root.get("creditsCurrent").getAsInt();
                }
            }
            return new DebugResults.Terminated(term, creditsCurrent);
        }
    }

    public static DebugResults.History history(Request req) throws Exception {
        try (Response res = HttpClientUtil.runSync(req)) {
            String body = (res.body() != null) ? res.body().string() : "";
            if (res.code() != 200) {
                throw new RuntimeException("DEBUG history failed: HTTP " + res.code() + " | " + body);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            boolean ok = obj.has("status") && "ok".equalsIgnoreCase(obj.get("status").getAsString());
            int runNumber = (obj.has("runNumber") && !obj.get("runNumber").isJsonNull())
                    ? obj.get("runNumber").getAsInt()
                    : 0 ;
            return new DebugResults.History(ok, runNumber);
        }
    }

    // --------------------- helpers ---------------------

    private static int parseRetryMs(String body, Response res) {
        try {
            if (body != null && !body.isEmpty()) {
                JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
                if (obj != null && obj.has("retryMs")) {
                    return Math.max(0, obj.get("retryMs").getAsInt());
                }
            }
        } catch (Throwable ignore) {}
        String ra = res.header("Retry-After");
        if (ra != null) {
            try { return Math.max(0, Integer.parseInt(ra) * 1000); } catch (Throwable ignore) {}
        }
        return 1500; // sane default
    }
}
