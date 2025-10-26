package client.requests.runtime;

import com.google.gson.JsonObject;
import execution.ExecutionRequestDTO;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;
import utils.JsonUtils;

public final class Debug {

    private Debug() {}

    /** POST /api/debug/init  (body: { degree, inputs, function? }) */
    public static Request init(ExecutionRequestDTO dto, String functionUserString) {
        JsonObject body = JsonUtils.GSON.toJsonTree(dto).getAsJsonObject();
        if (functionUserString != null && !functionUserString.isBlank()) {
            body.addProperty("function", functionUserString);
        }

        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_INIT)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    // POST /api/debug/init
// Body we send: { degree, inputs, generation, program, function? }
    public static Request init(ExecutionRequestDTO dto,
                               String programName,
                               String functionUserString) {
        JsonObject body = JsonUtils.GSON.toJsonTree(dto).getAsJsonObject();
        // tell the server which program this debug session is for
        if (programName != null && !programName.isBlank()) {
            body.addProperty("program", programName);
        }
        // if we're debugging a specific function, include its unique user string
        if (functionUserString != null && !functionUserString.isBlank()) {
            body.addProperty("function", functionUserString);
        }

        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_INIT)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** POST /api/debug/step  (body: { debugId }) */
    public static Request step(String debugId) {
        JsonObject body = new JsonObject();
        body.addProperty("debugId", debugId);

        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_STEP)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** POST /api/debug/stop  (body: { debugId }) — stop specific session */
    public static Request stop(String debugId) {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalArgumentException("debugId is required");
        }

        JsonObject body = new JsonObject();
        body.addProperty("debugId", debugId);

        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_STOP)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** POST /api/debug/resume  (body: { debugId }) — async accept (202) / busy (429) / locked (409) */
    public static Request resume(String debugId) {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalArgumentException("debugId is required");
        }

        JsonObject body = new JsonObject();
        body.addProperty("debugId", debugId);

        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_RESUME)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** POST /api/debug/terminated  (body: { debugId }) → { terminated: boolean } */
    public static Request terminated(String debugId) {
        JsonObject body = new JsonObject();
        body.addProperty(Constants.QP_DEBUG_ID, debugId);
        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_TERMINATED)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** POST /api/debug/history (body: ExecutionRequestDTO) */
    public static Request history(ExecutionRequestDTO dto) {
        JsonObject body = JsonUtils.GSON.toJsonTree(dto).getAsJsonObject();
        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_DEBUG_HISTORY)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** NEW: GET /api/debug/state?debugId=... → returns latest DebugStateDTO snapshot */
    public static Request state(String debugId) {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalArgumentException("debugId is required");
        }

        // Build URL with query parameter using OkHttp's HttpUrl for safety.
        HttpUrl url = HttpUrl.parse(Constants.BASE_URL + Constants.API_DEBUG_STATE)
                .newBuilder()
                .addQueryParameter(Constants.QP_DEBUG_ID, debugId)
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
