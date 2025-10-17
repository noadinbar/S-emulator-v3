package client.responses;

import com.google.gson.Gson;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import client.requests.ProgramByName;
import display.DisplayDTO;

import java.io.IOException;
import java.io.InputStreamReader;

public class ProgramByNameResponder {
    private static final Gson GSON = new Gson();

    public static DisplayDTO execute(String name) throws IOException {
        Request req = ProgramByName.build(name);
        try (Response rs = HttpClientUtil.runSync(req)) {
            if (!rs.isSuccessful()) {
                String body = rs.body() != null ? rs.body().string() : "";
                throw new IOException("Fetch program failed: HTTP " + rs.code() + " " + rs.message() +
                        (body.isBlank() ? "" : " – " + body));
            }
            if (rs.body() == null) throw new IOException("Empty body");
            try (InputStreamReader r = new InputStreamReader(rs.body().byteStream())) {
                DisplayDTO dto = GSON.fromJson(r, DisplayDTO.class);
                if (dto == null) throw new IOException("Invalid response");
                return dto;
            }
        }
    }
}
