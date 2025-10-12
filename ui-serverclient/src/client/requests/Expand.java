package client.requests;

import okhttp3.HttpUrl;
import okhttp3.Request;
import utils.Constants;

public class Expand {
    public static Request build(int degree) {
        HttpUrl base = HttpUrl.parse(Constants.BASE_URL + Constants.API_EXPAND);
        if (base == null) {
            throw new IllegalArgumentException("Invalid BASE_URL/API_EXPAND");
        }
        HttpUrl url = base.newBuilder()
                .addQueryParameter("degree", String.valueOf(degree))
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }

    /** Build GET /api/expand?degree=N&function={userString} */
    public static Request build(String functionUserString, int degree) {
        HttpUrl base = HttpUrl.parse(Constants.BASE_URL + Constants.API_EXPAND);
        if (base == null) {
            throw new IllegalArgumentException("Invalid BASE_URL/API_EXPAND");
        }
        HttpUrl url = base.newBuilder()
                .addQueryParameter("degree", String.valueOf(degree))
                .addQueryParameter("function", functionUserString)
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
