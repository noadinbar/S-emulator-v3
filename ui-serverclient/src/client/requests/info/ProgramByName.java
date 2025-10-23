package client.requests.info;

import okhttp3.HttpUrl;
import okhttp3.Request;
import utils.Constants;

public class ProgramByName {
    public static Request build(String name) {
        HttpUrl url = HttpUrl.parse(Constants.BASE_URL + Constants.API_PROGRAM_BY_NAME)
                .newBuilder()
                .addQueryParameter("name", name)
                .build();
        return new Request.Builder().url(url).get().build();
    }
}
