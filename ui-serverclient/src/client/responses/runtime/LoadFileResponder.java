package client.responses.runtime;

import com.google.gson.Gson;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import client.requests.runtime.LoadFile;            // נשאר השם שלך לבקשה
import display.UploadResultDTO;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class LoadFileResponder {
    private static final Gson GSON = new Gson();

    public static UploadResultDTO execute(Path xmlPath) throws IOException {
        Request req = LoadFile.build(xmlPath);
        try (Response rs = HttpClientUtil.runSync(req)) {
            if (!rs.isSuccessful()) {
                String body = rs.body() != null ? rs.body().string() : "";
                throw new IOException("Upload failed: HTTP " + rs.code() + " " + rs.message()
                        + (body.isBlank() ? "" : " – " + body));
            }
            if (rs.body() == null) throw new IOException("Empty body");
            try (InputStreamReader r = new InputStreamReader(rs.body().byteStream())) {
                UploadResultDTO dto = GSON.fromJson(r, UploadResultDTO.class);
                if (dto == null) throw new IOException("Invalid response");
                if (!dto.ok) throw new IOException(dto.error != null ? dto.error : "Upload failed");
                return dto;
            }
        }
    }
}
