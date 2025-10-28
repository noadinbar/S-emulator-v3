package client.responses.runtime;

import com.google.gson.reflect.TypeToken;
import execution.RunHistoryEntryDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

import java.lang.reflect.Type;
import java.util.List;

public class HistoryResponder {
    public static List<RunHistoryEntryDTO> get(Request req) throws Exception {
        try (Response rs = HttpClientUtil.runSync(req)) {
            String body = rs.body() != null ? rs.body().string() : "";
            if (rs.code() != 200) {
                throw new RuntimeException("HISTORY failed: HTTP " + rs.code() + " | " + body);
            }
            Type listType = new TypeToken<List<RunHistoryEntryDTO>>() {}.getType();
            List<RunHistoryEntryDTO> parsed =
                    JsonUtils.GSON.fromJson(body, listType);
            return parsed;
        }
    }
}
