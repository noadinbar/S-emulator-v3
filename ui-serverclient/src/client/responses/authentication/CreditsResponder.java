package client.responses.authentication;

import client.requests.authentication.Credits;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;

import java.io.IOException;

/** Client helper for POST /api/credits/charge. */
public class CreditsResponder {
    private static final Gson GSON = new Gson();

    public static JsonObject charge(int amount) throws IOException {
        Request req = Credits.charge(amount);
        Call call = HttpClientUtil.get().newCall(req);
        try (Response res = call.execute()) {
            if (!res.isSuccessful()) {
                String msg = "Charge failed: HTTP " + res.code();
                throw new IOException(msg);
            }
            if (res.body() == null) {
                throw new IOException("Charge failed: empty body");
            }
            String json = res.body().string();
            return GSON.fromJson(json, JsonObject.class);
        }
    }
}
