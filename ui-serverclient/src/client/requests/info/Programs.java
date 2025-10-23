package client.requests.info;

import okhttp3.Request;
import utils.Constants;

public class Programs {
    public static Request build() {
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_PROGRAMS) // "/api/programs"
                .get()
                .build();
    }
}
