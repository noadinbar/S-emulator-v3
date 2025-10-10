package client.responses;

import utils.HttpClientUtil;
import client.requests.LoadFile;
import com.google.gson.JsonSyntaxException;
import display.DisplayDTO;        // <-- use your actual DTO package
import okhttp3.Request;
import okhttp3.Response;
import utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Responder for the "load file" flow:
 *  - builds the request via client.requests.LoadFile
 *  - runs it through HttpClientUtil
 *  - parses JSON -> DisplayDTO using JsonUtils.GSON
 */
public class LoadFileResponder {

    /**
     * Sends the selected XML file to the server (POST /api/load) and parses the JSON response.
     * @param xmlPath local filesystem path to the XML
     * @return DisplayDTO ("as-is" program view)
     * @throws IOException on network/HTTP errors
     * @throws JsonSyntaxException if the JSON doesn't match DisplayDTO
     */
    public static DisplayDTO execute(Path xmlPath) throws IOException, JsonSyntaxException {
        // 1) Build the multipart/form-data request
        Request request = LoadFile.build(xmlPath);

        // 2) Execute synchronously via the shared OkHttp client
        try (Response rs = HttpClientUtil.runSync(request)) {
            int code = rs.code();
            String body = rs.body() != null ? rs.body().string() : "";

            // Expecting 201 Created + JSON body (DisplayDTO)
            if (code != 201) {
                throw new IOException("LOAD failed: HTTP " + code + " | " + body);
            }

            // 3) JSON -> DTO (round-trip complete)
            return JsonUtils.GSON.fromJson(body, DisplayDTO.class);
        }
    }
}
