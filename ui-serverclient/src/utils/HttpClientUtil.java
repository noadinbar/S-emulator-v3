package utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Callback;
import utils.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class HttpClientUtil {
    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
            .build();

    private HttpClientUtil() {}

    public static OkHttpClient get() { return CLIENT; }

    public static Response runSync(Request req) throws IOException {
        return CLIENT.newCall(req).execute();
    }

    public static void runAsync(Request req, Callback cb) {
        CLIENT.newCall(req).enqueue(cb);
    }
}
