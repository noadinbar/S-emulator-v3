// ui-serverclient/src/client/responses/UsersResponder.java
package client.responses.authentication;

import client.requests.authentication.Users;
import com.google.gson.reflect.TypeToken;
import users.UserTableRowDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class UsersResponder {
    private UsersResponder() { }
    private static final Type LIST_TYPE = new TypeToken<List<UserTableRowDTO>>() {}.getType();

    public static List<UserTableRowDTO> list() throws IOException {
        Request req = Users.build();
        try (Response rs = HttpClientUtil.runSync(req)) {
            if (!rs.isSuccessful()) return Collections.emptyList();
            String body = rs.body() != null ? rs.body().string() : "[]";
            List<UserTableRowDTO> rows = JsonUtils.GSON.fromJson(body, LIST_TYPE);
            return rows != null ? rows : Collections.emptyList();
        }
    }

    public static List<UserTableRowDTO> parse(Response response) throws IOException {
        try (Response rs = response) {
            String body = rs.body() != null ? rs.body().string() : "[]";
            List<UserTableRowDTO> rows = JsonUtils.GSON.fromJson(body, LIST_TYPE);
            return rows != null ? rows : Collections.emptyList();
        }
    }
}
