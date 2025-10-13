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

    /** נוחות: init על התוכנית הראשית (ללא function) */
    public static Request init(ExecutionRequestDTO dto) {
        return init(dto, null);
    }

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
}
