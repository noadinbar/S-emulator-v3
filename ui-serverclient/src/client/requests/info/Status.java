// client/requests/info/Status.java
package client.requests.info;

import okhttp3.HttpUrl;
import okhttp3.Request;
import utils.Constants;

public class Status {

    /**
     * Build GET /api/status?program=<programName>
     */
    public static Request build(String programName) {
        HttpUrl url = HttpUrl.parse(Constants.BASE_URL + Constants.API_STATUS)
                .newBuilder()
                .addQueryParameter("program", programName)
                .build();

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
