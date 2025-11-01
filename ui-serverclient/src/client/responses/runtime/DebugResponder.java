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
        try (Response rs = HttpClientUtil.runSync(req)) {
            String body = rs.body() != null ? rs.body().string() : "";
            int code = rs.code();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("STEP failed: HTTP " + code + " | " + body);
            }

            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);

            DebugStepDTO stepDto = null;
            if (obj != null && obj.has("step") && !obj.get("step").isJsonNull()) {
                stepDto = JsonUtils.GSON.fromJson(obj.get("step"), DebugStepDTO.class);
            }

            int creditsCurrent = 0;
            int creditsUsed = 0;

            if (obj != null && obj.has("credits") && !obj.get("credits").isJsonNull()) {
                JsonObject creditsObj = obj.getAsJsonObject("credits");
                if (creditsObj != null) {
                    if (creditsObj.has("current") && !creditsObj.get("current").isJsonNull()) {
                        creditsCurrent = creditsObj.get("current").getAsInt();
                    }
                    if (creditsObj.has("used") && !creditsObj.get("used").isJsonNull()) {
                        creditsUsed = creditsObj.get("used").getAsInt();
                    }
                }
            }

            if (creditsCurrent == 0 && obj != null && obj.has("creditsCurrent") && !obj.get("creditsCurrent").isJsonNull()) {
                creditsCurrent = obj.get("creditsCurrent").getAsInt();
            }
            if (creditsUsed == 0 && obj != null && obj.has("creditsUsed") && !obj.get("creditsUsed").isJsonNull()) {
                creditsUsed = obj.get("creditsUsed").getAsInt();
            }

            // terminated / outOfCredits
            boolean terminated = false;
            if (obj != null && obj.has("terminated") && !obj.get("terminated").isJsonNull()) {
                terminated = obj.get("terminated").getAsBoolean();
            }

            boolean outOfCredits = false;
            if (obj != null && obj.has("outOfCredits") && !obj.get("outOfCredits").isJsonNull()) {
                outOfCredits = obj.get("outOfCredits").getAsBoolean();
            }

            return new DebugResults.StepResult(
                    stepDto,
                    creditsCurrent,
                    creditsUsed,
                    terminated,
                    outOfCredits
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
        try (Response rs = HttpClientUtil.runSync(req)) {
            String body = rs.body() != null ? rs.body().string() : "";
            int code = rs.code();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("TERMINATED failed: HTTP " + code + " | " + body);
            }

            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);

            boolean terminated = false;
            if (obj != null && obj.has("terminated")) {
                terminated = obj.get("terminated").getAsBoolean();
            }

            int creditsCurrent = 0;
            if (obj != null && obj.has("creditsCurrent")) {
                creditsCurrent = obj.get("creditsCurrent").getAsInt();
            }

            boolean outOfCredits = false;
            if (obj != null && obj.has("outOfCredits")) {
                outOfCredits = obj.get("outOfCredits").getAsBoolean();
            }

            return new DebugResults.Terminated(
                    terminated,
                    creditsCurrent,
                    outOfCredits
            );
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
