package utils;

public class Constants {
    public static final String ATTR_DISPLAY_API = "DISPLAY_API";
    public static final String ATTR_MODE        = "MODE";
    public static final String MODE_IDLE  = "idle";
    public static final String MODE_RUN   = "run";
    public static final String MODE_DEBUG = "debug";
    public static final String API_LOAD                  = "/api/load-file";
    public static final String API_PROGRAM               = "/api/program";
    public static final String API_MAX_DEGREE            = "/api/max-degree";
    public static final String API_EXPAND                = "/api/expand";
    public static final String API_HISTORY               = "/api/history";
    public static final String API_MODE                  = "/api/mode";
    public static final String API_DEBUG_INIT            = "/api/debug/init";
    public static final String API_DEBUG_STEP            = "/api/debug/step";
    public static final String API_DEBUG_TERMINATED      = "/api/debug/terminated";
    public static final String API_DEBUG_STOP            = "/api/debug/stop";
    public static final String API_FUNCTIONS             = "/api/functions";
    public static final String API_FUNCTION_PROGRAM      = "/api/functions/%s/program";
    public static final String API_FUNCTION_EXPAND       = "/api/functions/%s/expand";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON   = "application/json";
    public static final String CONTENT_TYPE_XML    = "application/xml";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String CHARSET_UTF8        = "UTF-8";
    public static final String PART_FILE   = "file";    // multipart field name for uploads
    public static final String QP_NAME     = "name";    // e.g., ?name=...
    public static final String QP_DEGREE   = "degree";  // e.g., ?degree=2
    public static final String JSON_ERROR  = "error";
    public static final String JSON_STATUS = "status";
}
