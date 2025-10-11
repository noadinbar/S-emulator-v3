package client.responses;

import client.requests.Functions;
import com.google.gson.reflect.TypeToken;
import display.DisplayDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class FunctionsResponder {
    public static List<String> list() throws IOException {
        Request req = Functions.list();
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "";
            if (code != 200) throw new IOException("GET /api/functions failed: " + code + " | " + body);
            Type t = new TypeToken<List<String>>(){}.getType();
            return JsonUtils.GSON.fromJson(body, t);
        }
    }

    public static DisplayDTO program(String key) throws IOException {
        Request req = Functions.program(key);
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "";
            if (code != 200) throw new IOException("GET /api/functions/{key}/program failed: " + code + " | " + body);
            return JsonUtils.GSON.fromJson(body, DisplayDTO.class);
        }
    }
}
