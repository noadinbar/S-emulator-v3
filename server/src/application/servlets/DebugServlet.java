package application.servlets;

import api.DebugAPI;
import api.DisplayAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import display.DisplayDTO;
import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import application.execution.ExecutionTaskManager;
import application.execution.JobSubmitResult;

import static utils.Constants.*;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(
        name = "DebugServlet",
        urlPatterns = {
                API_DEBUG_INIT,
                API_DEBUG_STEP,
                API_DEBUG_RESUME,
                API_DEBUG_STOP,
                API_DEBUG_TERMINATED,
                API_DEBUG_HISTORY
        }
)
public class DebugServlet extends HttpServlet {

    private static final String ATTR_DEBUG_SESSIONS = "debug.sessions";
    private static final String ATTR_DBG_BUSY       = "dbgBusy";
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String route = req.getServletPath(); // כי מיפוי מלא
        switch (route) {
            case API_DEBUG_INIT   -> handleInit(req, resp);
            case API_DEBUG_STEP   -> handleStep(req, resp);
            case API_DEBUG_RESUME -> handleResume(req, resp);
            case API_DEBUG_STOP   -> handleStop(req, resp);
            case API_DEBUG_TERMINATED  -> handleTerminated(req, resp);
            case API_DEBUG_HISTORY    -> handleHistory(req, resp);
            default -> writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                    "Unknown debug route: " + route);
        }
    }

    // -------- Handlers --------

    /**
     * POST /api/debug/init
     * Body JSON: { degree: number, inputs: number[], function?: string }
     * New behavior (async): 202 {debugId} | 429 {retryMs}
     */
    private void handleInit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DisplayAPI root = getRootOrError(resp);
        if (root == null) return;

        JsonObject in = readJson(req);
        if (in == null) in = new JsonObject();

        // בלי קבוע: קוראים ישירות את "function"
        String functionKey = (in.has("function") && !in.get("function").isJsonNull())
                ? in.get("function").getAsString()
                : null;

        ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);
        if (execReq == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing execution request body.");
            return;
        }
        int degree = Math.max(0, execReq.getDegree());

        DisplayAPI target = resolveTarget(root, functionKey);
        if (target == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                    "Function not found: " + functionKey);
            return;
        }

        // מזהה סשן מראש כדי שהלקוח ידע על מה לפולינג
        String id = UUID.randomUUID().toString();

        // מגישים ל-pool: init דיבאג ירוץ ברקע; על עומס -> 429; על קבלה -> 202
        JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugAPI dbg = target.debugForDegree(degree);
                DebugStateDTO state = dbg.init(execReq);

                // שמירת הסשן מוכן לצעדים/רזיום
                getSessions().put(id, dbg);
                // dbgBusy true כל עוד יש לפחות סשן אחד
                getServletContext().setAttribute(ATTR_DBG_BUSY, Boolean.TRUE);

                // אם תרצי לשמור snapshot ראשוני בצד השרת — אפשר פה במפה נפרדת
                // (לא חובה כרגע; ה-UI בדרך כלל ינהל את המצב לאחר הפול הראשון)
                // getSnapshots().put(id, state);

            } catch (Throwable t) {
                // במקרה של שגיאה, אין סשן פעיל
                getSessions().remove(id);
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
                // נזרוק כדי שיסומן כ-ERROR ברמת ה-job; הלקוח יקבל זאת דרך ה-/terminated/step בהמשך
                throw t;
            }
            return null; // ExecutionTaskManager לא משתמש בתוצאה כאן
        });

        if (!res.isAccepted()) {
            // עומס: 429 + Retry-After (בשניות) + גוף עם retryMs
            int retryMs = res.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);
            resp.setStatus(SC_TOO_MANY_REQUESTS);
            resp.setHeader("Retry-After", String.valueOf(retrySec));
            writeJson(resp, SC_TOO_MANY_REQUESTS,
                    gson.toJsonTree(new JsonObject() {{
                        addProperty("error", "busy");
                        addProperty("retryMs", retryMs);
                    }}));
            return;
        }

        // התקבל: לא מחזירים state (אין חסימה) — ה-UI יעשה polling לפי debugId
        JsonObject out = new JsonObject();
        out.addProperty("debugId", id);
        resp.setStatus(HttpServletResponse.SC_ACCEPTED); // 202
        writeJson(resp, HttpServletResponse.SC_ACCEPTED, out);
    }

    /** POST /api/debug/step  (body או query: debugId) — מחזיר DebugStepDTO (כרגע סינכרוני כמו שהיה) */
    private void handleStep(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // קבלת debugId או מה-query או מה-JSON body
        String debugId = req.getParameter("debugId");
        if (debugId == null || debugId.isBlank()) {
            JsonObject in = readJson(req);
            if (in != null && in.has("debugId") && !in.get("debugId").isJsonNull()) {
                debugId = in.get("debugId").getAsString();
            }
        }
        if (debugId == null || debugId.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().get(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        try {
            DebugStepDTO step = dbg.step();
            writeJson(resp, HttpServletResponse.SC_OK, step);

            // אם הסשן הסתיים לאחר הצעד — מנקים ומעדכנים dbgBusy
            boolean term = false;
            try { term = dbg.isTerminated(); } catch (Throwable ignore) { /* נסבול בשקט */ }
            if (term) {
                getSessions().remove(debugId);
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
            }
        } catch (Exception e) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Debug step failed: " + e.getMessage());
        }
    }

    private void handleResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = req.getParameter("debugId");
        JsonObject in = null;
        if (debugId == null || debugId.isBlank()) {
            in = readJson(req);
            if (in != null && in.has("debugId") && !in.get("debugId").isJsonNull()) {
                debugId = in.get("debugId").getAsString();
            }
        }
        if (debugId == null || debugId.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().get(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        try {
            int steps = 0;
            DebugStepDTO last = null;

            // ריצה עד סיום (ללא תקרה) — כרגע סינכרוני כמו שהיה
            while (!dbg.isTerminated()) {
                last = dbg.step();
                steps++;
            }

            getSessions().remove(debugId);
            boolean anyLeft = !getSessions().isEmpty();
            getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);

            JsonObject out = new JsonObject();
            out.addProperty("terminated", true);
            out.addProperty("steps", steps);
            out.add("lastState", gson.toJsonTree(last != null ? last.getNewState() : null));
            writeJson(resp, HttpServletResponse.SC_OK, out);
        } catch (Exception e) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Debug resume failed: " + e.getMessage());
        }
    }

    /** POST /api/debug/stop  – עוצר *רק* סשן ספציפי לפי debugId */
    private void handleStop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // קורא debugId או מה-query או מגוף ה-JSON
        String debugId = req.getParameter("debugId");
        if (debugId == null || debugId.isBlank()) {
            JsonObject in = readJson(req);
            if (in != null && in.has("debugId") && !in.get("debugId").isJsonNull()) {
                debugId = in.get("debugId").getAsString();
            }
        }

        if (debugId == null || debugId.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().remove(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        // עדכון dbgBusy לפי האם נשארו סשנים פתוחים
        boolean anyLeft = !getSessions().isEmpty();
        getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);

        JsonObject out = new JsonObject();
        out.addProperty("stopped", true);
        out.addProperty("debugId", debugId);
        out.addProperty("remaining", getSessions().size());
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }

    /** POST /api/debug/terminated (body/query: debugId) → { "terminated": boolean } */
    private void handleTerminated(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // קורא debugId מה-query או מה-JSON body
        String debugId = req.getParameter("debugId");
        if (debugId == null || debugId.isBlank()) {
            JsonObject in = readJson(req);
            if (in != null && in.has("debugId") && !in.get("debugId").isJsonNull()) {
                debugId = in.get("debugId").getAsString();
            }
        }
        if (debugId == null || debugId.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        boolean term;
        DebugAPI dbg = getSessions().get(debugId);

        if (dbg == null) {
            term = true;
        } else {
            boolean t = false;
            try { t = dbg.isTerminated(); } catch (Throwable ignore) { /* שקט */ }
            term = t;
            if (term) {
                getSessions().remove(debugId);
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("terminated", term);
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }

    private void handleHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DisplayAPI root = getRootOrError(resp);
        if (root == null) return;

        JsonObject in = readJson(req);
        if (in == null) in = new JsonObject();

        ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);
        if (execReq == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing execution request body.");
            return;
        }
        int degree = Math.max(0, execReq.getDegree());

        try {
            root.executionForDegree(degree).execute(execReq);
            JsonObject out = new JsonObject();
            out.addProperty("status", "ok");
            writeJson(resp, HttpServletResponse.SC_OK, out);
        } catch (Exception ex) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "debug history failed: " + ex.getMessage());
        }
    }


    // -------- Helpers --------
    private DisplayAPI getRootOrError(HttpServletResponse resp) throws IOException {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_API);
        if (!(obj instanceof DisplayAPI display)) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "No loaded program.");
            return null;
        }
        if (safeDisplay(display) == null) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "No loaded program.");
            return null;
        }
        return display;
    }

    private DisplayDTO safeDisplay(DisplayAPI display) {
        try { return display.getDisplay(); } catch (Throwable ignore) { return null; }
    }

    private DisplayAPI resolveTarget(DisplayAPI root, String functionKey) {
        if (functionKey == null || functionKey.isBlank()) return root;
        try {
            Map<String, DisplayAPI> map = root.functionDisplaysByUserString();
            return (map != null) ? map.get(functionKey) : null;
        } catch (Throwable t) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, DebugAPI> getSessions() {
        Object obj = getServletContext().getAttribute(ATTR_DEBUG_SESSIONS);
        if (obj instanceof Map<?, ?> m) return (Map<String, DebugAPI>) m;
        Map<String, DebugAPI> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DEBUG_SESSIONS, created);
        return created;
    }

    private JsonObject readJson(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        if (sb.length() == 0) return null;
        return gson.fromJson(sb.toString(), JsonObject.class);
    }
}
