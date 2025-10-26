package client.responses.runtime;

import display.ExpandDTO;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import utils.Constants;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.io.IOException;

public class ExpandResponder {

    // Expand for a full program (identified by its program name).
    public static ExpandDTO expandProgram(String programName, int degree) throws IOException {
        Request req = buildProgramRequest(programName, degree);
        return call(req);
    }

    // Expand for a single function (identified by its user-string).
    public static ExpandDTO expandFunction(String functionUserString, int degree) throws IOException {
        Request req = buildFunctionRequest(functionUserString, degree);
        return call(req);
    }

    // Shared HTTP execution logic.
    private static ExpandDTO call(Request req) throws IOException {
        try (Response res = HttpClientUtil.runSync(req)) {
            int code = res.code();
            String body = (res.body() != null) ? res.body().string() : "";
            if (code != 200) {
                throw new IOException("EXPAND failed: HTTP " + code + " | " + body);
            }
            return JsonUtils.GSON.fromJson(body, ExpandDTO.class);
        }
    }

    // Build GET /api/expand?degree=...&program=...
    private static Request buildProgramRequest(String programName, int degree) {
        HttpUrl base = HttpUrl.parse(Constants.BASE_URL + Constants.API_EXPAND);
        if (base == null) {
            throw new IllegalArgumentException("Invalid BASE_URL/API_EXPAND");
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("degree", String.valueOf(degree))
                .addQueryParameter("program", programName)
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    // Build GET /api/expand?degree=...&function=...
    private static Request buildFunctionRequest(String functionUserString, int degree) {
        HttpUrl base = HttpUrl.parse(Constants.BASE_URL + Constants.API_EXPAND);
        if (base == null) {
            throw new IllegalArgumentException("Invalid BASE_URL/API_EXPAND");
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("degree", String.valueOf(degree))
                .addQueryParameter(Constants.JSON_FUNCTION, functionUserString)
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
