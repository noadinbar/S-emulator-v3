package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServletUtils {

    // Gson via Builder (classroom style)
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /** Write success JSON (status + payload). */
    public static void writeJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        GSON.toJson(payload, resp.getWriter());
    }

    /** Write error JSON in a uniform shape: { "error": "<message>" }. */
    public static void writeJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON);
        String safe = message == null ? "" : message.replace("\"", "'");
        resp.getWriter().write("{\"" + Constants.JSON_ERROR + "\":\"" + safe + "\"}");
    }

}
