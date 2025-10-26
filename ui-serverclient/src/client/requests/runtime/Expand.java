package client.requests.runtime;

import com.google.gson.JsonObject;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;
import utils.JsonUtils;

/**
 * Build POST /api/expand
 * Body includes { degree, program, function? }
 */
public class Expand {

    public static Request build(int degree,
                                String programName,
                                String functionUserString) {

        JsonObject body = new JsonObject();
        body.addProperty("degree", degree);
        body.addProperty("program", programName);
        if (functionUserString != null && !functionUserString.isBlank()) {
            body.addProperty("function", functionUserString);
        }

        RequestBody rb = RequestBody.create(
                body.toString(),
                Constants.MEDIA_TYPE_JSON
        );

        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_EXPAND)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
