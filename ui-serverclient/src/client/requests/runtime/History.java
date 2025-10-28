package client.requests.runtime;

import okhttp3.HttpUrl;
import okhttp3.Request;
import utils.Constants;

public class History {
    public static Request build(String usernameFilter) {
        HttpUrl.Builder builder =
                HttpUrl.parse(Constants.BASE_URL + Constants.API_HISTORY)
                        .newBuilder();

        if (usernameFilter != null && !usernameFilter.isBlank()) {
            builder.addQueryParameter("user", usernameFilter);
        }
        return new Request.Builder()
                .url(builder.build())
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
