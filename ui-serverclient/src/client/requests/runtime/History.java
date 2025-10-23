package client.requests.runtime;

import okhttp3.HttpUrl;
import okhttp3.Request;
import utils.Constants;

public class History {
    public static Request build(String functionUserString) {
        HttpUrl.Builder b = HttpUrl.parse(Constants.BASE_URL + Constants.API_HISTORY).newBuilder();
        if (functionUserString != null && !functionUserString.isBlank()) {
            b.addQueryParameter("function", functionUserString);
        }
        return new Request.Builder()
                .url(b.build())
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
