package client.requests;

import okhttp3.Request;
import utils.Constants;

public class ProgramsList {
    public static Request build() {
        return new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_PROGRAMS) // "/api/programs"
                .get()
                .build();
    }
}
