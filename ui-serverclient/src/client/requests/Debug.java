package client.requests;

import com.google.gson.JsonObject;
import execution.ExecutionRequestDTO;
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

    /** POST /api/debug/stop  (body: { debugId }) — עוצר *סשן ספציפי* בלבד */
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

    /** POST /api/debug/resume  (body: { debugId }) — מריץ עד סיום */
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

}
