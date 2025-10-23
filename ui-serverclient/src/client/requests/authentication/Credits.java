package client.requests.authentication;

import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;

/** Build POST /api/credits/charge with {"amount": <int>} JSON body. */
public class Credits {
    public static Request charge(int amount) {
        String url = Constants.BASE_URL + Constants.API_CREDITS_CHARGE;
        String body = "{\"amount\":" + amount + "}";
        RequestBody rb = RequestBody.create(body, Constants.MEDIA_TYPE_JSON);
        return new Request.Builder()
                .url(url)
                .post(rb)
                .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                .build();
    }
}
