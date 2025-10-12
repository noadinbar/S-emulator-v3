package client.requests;

import com.google.gson.JsonObject;
import execution.ExecutionRequestDTO;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;
import utils.JsonUtils;

public class Execute {
    public static Request build(ExecutionRequestDTO dto, String functionUserString) {
        JsonObject body = JsonUtils.GSON.toJsonTree(dto).getAsJsonObject();
        if (functionUserString != null && !functionUserString.isBlank()) {
            body.addProperty("function", functionUserString);
        }
        RequestBody rb = RequestBody.create(body.toString(), Constants.MEDIA_TYPE_JSON);
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_EXECUTE)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
