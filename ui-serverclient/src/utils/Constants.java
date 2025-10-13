package utils;

import okhttp3.MediaType;

public class Constants {
    public static final String BASE_URL = "http://localhost:8080/server_Web_exploded";

    // Existing endpoints
    public static final String API_LOAD     = "/api/load-file";
    public static final String API_STATUS   = "/api/status";
    public static final String API_EXPAND   = "/api/expand";
    public static final String API_EXECUTE  = "/api/execute";
    public static final String API_FUNCTIONS= "/api/functions";
    public static final String API_HISTORY  = "/api/history";

    // === Debug endpoints ===
    public static final String API_DEBUG_INIT       = "/api/debug/init";        // POST
    public static final String API_DEBUG_STEP       = "/api/debug/step";        // POST
    public static final String API_DEBUG_TERMINATE  = "/api/debug/terminate";   // GET
    public static final String API_DEBUG_STOP       = "/api/debug/stop";        // POST

    // Headers / Content types
    public static final String HEADER_ACCEPT     = "Accept";
    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final MediaType MEDIA_TYPE_XML  = MediaType.parse("application/xml");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    // Query params / JSON keys (נוח לאחידות)
    public static final String QP_DEBUG_ID   = "debugId";
    public static final String JSON_FUNCTION = "function";
}
