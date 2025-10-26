package client.responses.info;

import utils.HttpClientUtil;
import client.requests.info.Status;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class StatusResponder {
    private static final Gson GSON = new Gson();

    public static JsonObject get(String programName) throws IOException {
        Request req = Status.build(programName);
        try (Response res = HttpClientUtil.runSync(req)) {
            if (!res.isSuccessful()) {
                throw new IOException("Status failed: HTTP " + res.code());
            }
            if (res.body() == null) {
                throw new IOException("Status failed: empty body");
            }
            String json = res.body().string();
            return GSON.fromJson(json, JsonObject.class);
        }
    }
}
