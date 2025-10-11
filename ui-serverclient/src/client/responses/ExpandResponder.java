package client.responses;

import client.requests.Expand;
import display.ExpandDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.io.IOException;

public class ExpandResponder {

    public static ExpandDTO execute(int degree) throws IOException {
        return execute(null, degree);
    }

    public static ExpandDTO execute(String functionUserString, int degree) throws IOException {
        final Request req = (functionUserString == null || functionUserString.isBlank())
                ? Expand.build(degree)
                : Expand.build(functionUserString, degree);

        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "";
            if (code != 200) {
                throw new IOException("EXPAND failed: HTTP " + code + " | " + body);
            }
            return JsonUtils.GSON.fromJson(body, ExpandDTO.class);
        }
    }
}
