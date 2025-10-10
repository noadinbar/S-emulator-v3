package client.responses;

import utils.HttpClientUtil;
import client.requests.Status;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class StatusResponder {
    private static final Gson GSON = new Gson();

    /**
     * Synchronous call to /api/status that returns a JsonObject.
     */
    public static JsonObject get() throws IOException {
        Request req = Status.build();
        Call call = HttpClientUtil.get().newCall(req);
        try (Response res = call.execute()) {
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
