package client.responses.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import execution.ExecutionDTO;
import execution.ExecutionPollDTO;
import execution.ExecutionRequestDTO;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.util.concurrent.TimeUnit;

public class ExecuteResponder {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // ---- SUBMIT: POST /api/execute -> jobId ----
    public static Request buildSubmitRequest(String executeEndpointUrl,
                                                     ExecutionRequestDTO dto) {
        return buildSubmitRequest(executeEndpointUrl, dto, null);
    }

    public static Request buildSubmitRequest(String executeEndpointUrl,
                                                     ExecutionRequestDTO dto,
                                                     String functionUserString) {
        JsonObject root = (JsonObject) JsonUtils.GSON.toJsonTree(dto);
        if (functionUserString != null && !functionUserString.isBlank()) {
            root.addProperty("function", functionUserString);
        }
        RequestBody body = RequestBody.create(JSON, JsonUtils.GSON.toJson(root));
        return new Request.Builder()
                .url(executeEndpointUrl)
                .post(body)
                .build();
    }

    public static JobSubmitResult submit(Request postSubmit) throws Exception {
        try (Response rs = HttpClientUtil.runSync(postSubmit)) {
            String body = rs.body() != null ? rs.body().string() : "";
            int code = rs.code();
            if (code == 429) {
                int retryMs = 500;
                try {
                    JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
                    if (obj != null && obj.has("retryMs")) retryMs = obj.get("retryMs").getAsInt();
                } catch (Exception ignore) {}
                String ra = rs.header("Retry-After");
                if (ra != null) {
                    try { retryMs = Math.max(retryMs, (int)(Double.parseDouble(ra) * 1000.0)); } catch (Exception ignore) {}
                }
                return JobSubmitResult.busy(retryMs);
            }

            if (code >= 200 && code < 300) {
                JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
                if (obj != null && obj.has("jobId")) {
                    return JobSubmitResult.accepted(obj.get("jobId").getAsString());
                }
                throw new RuntimeException("SUBMIT malformed success: " + body);
            }

            throw new RuntimeException("SUBMIT failed: HTTP " + code + " | " + body);
        }
    }

    // NEW: program + function (function optional)
    public static Request buildSubmitRequest(String executeEndpointUrl,
                                             ExecutionRequestDTO dto,
                                             String programName,
                                             String functionUserString) {
        JsonObject root = (JsonObject) JsonUtils.GSON.toJsonTree(dto);
        if (programName != null && !programName.isBlank()) {
            root.addProperty("program", programName);
        }
        if (functionUserString != null && !functionUserString.isBlank()) {
            root.addProperty("function", functionUserString);
        }
        RequestBody body = RequestBody.create(JSON, JsonUtils.GSON.toJson(root));
        return new Request.Builder()
                .url(executeEndpointUrl)
                .post(body)
                .build();
    }


    // ---- POLL: GET /api/execute?jobId=... -> ExecutionPollDTO ----
    public static Request buildPollRequest(String executeEndpointUrl, String jobId) {
        HttpUrl url = HttpUrl.parse(executeEndpointUrl).newBuilder()
                .addQueryParameter("jobId", jobId)
                .build();
        return new Request.Builder().url(url).get().build();
    }

    public static ExecutionPollDTO poll(Request getPoll) throws Exception {
        try (Response rs = HttpClientUtil.runSyncWithTimeout(getPoll, 3, TimeUnit.SECONDS)) {
            String body = rs.body() != null ? rs.body().string() : "";
            if (rs.code() < 200 || rs.code() >= 300) {
                throw new RuntimeException("POLL failed: HTTP " + rs.code() + " | " + body);
            }
            JsonObject obj = JsonUtils.GSON.fromJson(body, JsonObject.class);
            if (obj == null || !obj.has("status")) {
                throw new RuntimeException("POLL malformed response: " + body);
            }

            // status
            String statusStr = obj.get("status").getAsString();
            ExecutionPollDTO.Status status;
            try {
                status = ExecutionPollDTO.Status.valueOf(statusStr);
            } catch (IllegalArgumentException ex) {
                status = ExecutionPollDTO.Status.ERROR; // fallback on unknown
            }

            // result (when DONE)
            ExecutionDTO dto = null;
            if (status == ExecutionPollDTO.Status.DONE && obj.has("result")) {
                JsonElement res = obj.get("result");
                if (res != null && !res.isJsonNull()) {
                    dto = JsonUtils.GSON.fromJson(res, ExecutionDTO.class);
                }
            }

            // error (when ERROR)
            String err = null;
            if (obj.has("error") && !obj.get("error").isJsonNull()) {
                JsonElement er = obj.get("error");
                err = er.isJsonPrimitive() ? er.getAsString() : er.toString(); // stringify object if needed
            }
            return new ExecutionPollDTO(status, dto, err);
        }
    }

    // ---- CANCEL: DELETE /api/execute?jobId=... ----
    public static Request buildCancelRequest(String executeEndpointUrl, String jobId) {
        HttpUrl url = HttpUrl.parse(executeEndpointUrl).newBuilder()
                .addQueryParameter("jobId", jobId)
                .build();
        return new Request.Builder().url(url).delete().build();
    }

    public static void cancel(Request deleteCancel) throws Exception {
        try (Response rs = HttpClientUtil.runSync(deleteCancel)) {
            String body = rs.body() != null ? rs.body().string() : "";
            if (rs.code() < 200 || rs.code() >= 300) {
                throw new RuntimeException("CANCEL failed: HTTP " + rs.code() + " | " + body);
            }
        }
    }
}
