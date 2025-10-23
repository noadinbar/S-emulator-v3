package client.responses.authentication;

import com.google.gson.Gson;
import okhttp3.Request;
import okhttp3.Response;
import client.requests.authentication.Login;
import users.LoginDTO;
import utils.HttpClientUtil;

import java.io.IOException;
import java.io.InputStreamReader;

public class LoginResponder {
    private static final Gson GSON = new Gson();

    public static LoginDTO execute(String username) throws IOException {
        Request req = Login.build(username);
        try (Response rs = HttpClientUtil.runSync(req)) {
            if (!rs.isSuccessful()) {
                String errBody = rs.body() != null ? rs.body().string() : "";
                String msg = "HTTP " + rs.code() + " " + rs.message();
                if (!errBody.isBlank()) msg += " – " + errBody;
                throw new IOException(msg);
            }

            if (rs.body() == null) throw new IOException("Empty response body");
            try (InputStreamReader reader = new InputStreamReader(rs.body().byteStream())) {
                LoginDTO dto = GSON.fromJson(reader, LoginDTO.class);
                if (dto == null) throw new IOException("Invalid response");
                if (!dto.ok) throw new IOException(dto.error != null && !dto.error.isBlank() ? dto.error : "Login failed");
                return dto;
            }
        }
    }
}
