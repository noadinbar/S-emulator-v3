package client.requests.authentication;

import okhttp3.Request;
import utils.Constants;

public final class Users {
    private Users() { }
    public static Request build() {
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_USERS)
                .get()
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
