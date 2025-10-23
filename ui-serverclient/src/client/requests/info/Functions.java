package client.requests.info;

import okhttp3.Request;
import utils.Constants;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class Functions {
    private Functions() {}

    /** GET /api/functions/keys -> List<String> of function user-strings */
    public static Request list() {
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_FUNCTIONS_KEYS)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** GET /api/functions -> List<FunctionRowDTO> (rows for the table) */
    public static Request rows() {
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_FUNCTIONS)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** GET /api/functions/{key}/program -> DisplayDTO */
    public static Request program(String key) {
        String enc = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String url = String.format(Constants.BASE_URL + Constants.API_FUNCTION_PROGRAM, enc);
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
