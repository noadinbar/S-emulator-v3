package client.responses;

import execution.ExecutionDTO;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;
import utils.JsonUtils;

public class ExecuteResponder {
    public static ExecutionDTO execute(Request req) throws Exception {
        try (Response rs = HttpClientUtil.runSync(req)) {
            String body = rs.body() != null ? rs.body().string() : "";
            if (rs.code() != 200) {
                throw new RuntimeException("EXECUTE failed: HTTP " + rs.code() + " | " + body);
            }
            return JsonUtils.GSON.fromJson(body, ExecutionDTO.class);
        }
    }
}
