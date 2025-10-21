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
    public static final String API_DEBUG_INIT       = "/api/debug/init";
    public static final String API_DEBUG_STEP       = "/api/debug/step";
    public static final String API_DEBUG_TERMINATED  = "/api/debug/terminated";
    public static final String API_DEBUG_STOP       = "/api/debug/stop";
    public static final String API_DEBUG_RESUME       = "/api/debug/resume";
    public static final String API_DEBUG_HISTORY   = "/api/debug/history";
    public static final String API_PROGRAMS= "/api/programs";
    public static final String API_PROGRAM_BY_NAME= "/api/programs/by-name";
    public static final String API_FUNCTIONS_KEYS    = "/api/functions/keys";     // רק רשימת user-strings
    public static final String API_FUNCTION_PROGRAM  = "/api/functions/%s/program"; // DisplayDTO של הפונקציה/התוכנית
    public static final String API_USERS = "/api/users";

    // Headers / Content types
    public static final String HEADER_ACCEPT     = "Accept";
    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final MediaType MEDIA_TYPE_XML  = MediaType.parse("application/xml");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    // Query params / JSON keys (נוח לאחידות)
    public static final String QP_DEBUG_ID   = "debugId";
    public static final String JSON_FUNCTION = "function";

    public static final int REFRESH_RATE_MS = 1000;
}
