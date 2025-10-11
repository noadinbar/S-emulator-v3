package client.requests;

import okhttp3.Request;
import utils.Constants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Functions {
    public static Request list() {
        String url = Constants.BASE_URL + Constants.API_FUNCTIONS;
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    public static Request program(String key) {
        String url = Constants.BASE_URL + Constants.API_FUNCTIONS + "/" + enc(key) + "/program";
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, StandardCharsets.UTF_8.toString()); }
        catch (UnsupportedEncodingException e) { return s; }
    }
}
