// client/requests/Status.java
package client.requests;

import okhttp3.Request;
import utils.Constants;

public class Status {
    /**
     * Build GET /api/status request (no body).
     */
    public static Request build() {
        String url = Constants.BASE_URL + Constants.API_STATUS;
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
