package client.responses;

import client.requests.Functions;
import com.google.gson.reflect.TypeToken;
import display.DisplayDTO;
import display.FunctionRowDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Unified responder for all Functions-related endpoints:
 * - list():           GET /api/functions/keys -> List<String> user-strings
 * - program(key):     GET /api/functions/{key}/program -> DisplayDTO
 * - rowsFetch():      GET /api/functions -> List<FunctionRowDTO> (sync)
 * - rowsParse(resp):  parse Response -> List<FunctionRowDTO> (async refresher)
 */
public final class FunctionsResponder {
    private FunctionsResponder() {}

    /** GET /api/functions/keys -> List<String> of function user-strings */
    public static List<String> list() throws IOException {
        Request req = Functions.list();
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "[]";
            if (code != 200)
                throw new IOException("GET /api/functions/keys failed: " + code + " | " + body);
            Type t = new TypeToken<List<String>>() {}.getType();
            List<String> keys = JsonUtils.GSON.fromJson(body, t);
            return keys != null ? keys : Collections.emptyList();
        }
    }

    /** GET /api/functions/{key}/program -> DisplayDTO */
    public static DisplayDTO program(String key) throws IOException {
        Request req = Functions.program(key);
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "{}";
            if (code != 200)
                throw new IOException("GET /api/functions/{key}/program failed: " + code + " | " + body);
            return JsonUtils.GSON.fromJson(body, DisplayDTO.class);
        }
    }

    /** GET /api/functions -> List<FunctionRowDTO> (sync fetch for table rows) */
    public static List<FunctionRowDTO> rowsFetch() throws IOException {
        Request req = Functions.rows();
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = res.body() != null ? res.body().string() : "[]";
            if (code != 200) return Collections.emptyList();
            Type t = new TypeToken<List<FunctionRowDTO>>() {}.getType();
            List<FunctionRowDTO> rows = JsonUtils.GSON.fromJson(body, t);
            return rows != null ? rows : Collections.emptyList();
        }
    }

    /** Parse List<FunctionRowDTO> from an OkHttp Response (used by the refresher async callback). */
    public static List<FunctionRowDTO> rowsParse(Response response) throws IOException {
        try (Response res = response) {
            String body = res.body() != null ? res.body().string() : "[]";
            Type t = new TypeToken<List<FunctionRowDTO>>() {}.getType();
            List<FunctionRowDTO> rows = JsonUtils.GSON.fromJson(body, t);
            return rows != null ? rows : Collections.emptyList();
        }
    }
}
