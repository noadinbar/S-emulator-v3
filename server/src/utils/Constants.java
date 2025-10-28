package utils;

public class Constants {
    public static final String ATTR_DISPLAY_API = "DISPLAY_API";
    public static final String ATTR_MODE        = "MODE";
    public static final String ATTR_DEBUG_SESSIONS  = "debug.sessions";
    public static final String ATTR_DEBUG_LOCKS     = "debug.session.locks"; // per-session mutexes
    public static final String ATTR_DEBUG_SNAPSHOTS = "debug.snapshots";     // latest DebugStateDTO per debugId
    public static final String ATTR_DBG_BUSY        = "dbgBusy";
    public static final String ATTR_DISPLAY_REGISTRY = "display.registry";
    public static final String ATTR_DEBUG_META = "debugMeta";
    public static final String MODE_IDLE  = "idle";
    public static final String MODE_RUN   = "run";
    public static final String MODE_DEBUG = "debug";
    public static final String API_STATUS                = "/api/status";
    public static final String API_LOAD                  = "/api/load-file";
    public static final String API_EXPAND                = "/api/expand";
    public static final String API_EXECUTE               = "/api/execute";
    public static final String API_HISTORY               = "/api/history";
    public static final String API_DEBUG_INIT      = "/api/debug/init";
    public static final String API_DEBUG_STEP      = "/api/debug/step";
    public static final String API_DEBUG_RESUME   = "/api/debug/resume";  // POST
    public static final String API_DEBUG_STOP = "/api/debug/stop";
    public static final String API_DEBUG_TERMINATED = "/api/debug/terminated";
    public static final String API_DEBUG_HISTORY   = "/api/debug/history";
    public static final String API_DEBUG_STATE = "/api/debug/state";
    public static final String API_FUNCTIONS             = "/api/functions";
    public static final String API_FUNCTION_PROGRAM      = "/api/functions/%s/program";
    public static final String API_FUNCTION_EXPAND       = "/api/functions/%s/expand";
    public static final String API_CREDITS_CHARGE = "/api/credits/charge";
    public static final String API_LOGIN  = "/api/login";
    public static final String API_WHOAMI = "/api/whoami";
    public static final String API_PROGRAMS = "/api/programs";
    public static final String API_PROGRAM_BY_NAME= "/api/programs/by-name";
    public static final String API_USERS = "/api/users";

    public static final String SESSION_USERNAME = "username";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON   = "application/json";
    public static final String CONTENT_TYPE_XML    = "application/xml";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String CHARSET_UTF8        = "UTF-8";
    public static final String PART_FILE   = "file";
    public static final String QP_NAME     = "name";
    public static final String QP_DEGREE   = "degree";
    public static final String JSON_ERROR  = "error";
    public static final String JSON_STATUS = "status";
    public static final int PROGRAMS_REFRESH_RATE_MS = 2000;
    public static final int SC_TOO_MANY_REQUESTS = 429;
}
