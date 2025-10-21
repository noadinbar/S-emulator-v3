package utils;

import okhttp3.*;
import utils.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class HttpClientUtil {
    private static final OkHttpClient CLIENT =
            new OkHttpClient.Builder()
                    .cookieJar(new SimpleCookieManager())
                    .build();

    private HttpClientUtil() {}

    public static OkHttpClient get() { return CLIENT; }

    public static Response runSync(Request req) throws IOException {
        return CLIENT.newCall(req).execute();
    }

    public static void runAsync(Request req, Callback cb) {
        CLIENT.newCall(req).enqueue(cb);
    }

    public static Response runSyncWithTimeout(Request req, long value, TimeUnit unit) throws IOException {
        Call call = CLIENT.newCall(req);
        call.timeout().timeout(value, unit);
        return call.execute();
    }
}
