package client.requests.authentication;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;

public class Login {
    public static Request build(String username) {
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .build();

        return new Request.Builder()
                .url(Constants.BASE_URL + "/api/login")
                .post(body)
                .build();
    }
}
