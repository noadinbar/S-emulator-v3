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
                API_DEBUG_TERMINATED
        }
)
public class DebugServlet extends HttpServlet {

    private static final String ATTR_DEBUG_SESSIONS = "debug.sessions";
    private static final String ATTR_DBG_BUSY       = "dbgBusy";

    private final Gson gson = new Gson();

    // -------- Routing --------
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String route = req.getServletPath(); // כי מיפוי מלא
        switch (route) {
            case API_DEBUG_INIT   -> handleInit(req, resp);
            case API_DEBUG_STEP   -> handleStep(req, resp);
            case API_DEBUG_RESUME -> handleResume(req, resp);
            case API_DEBUG_STOP   -> handleStop(req, resp);
            case API_DEBUG_TERMINATED  -> handleTerminated(req, resp);
            default -> writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                    "Unknown debug route: " + route);
        }
    }

    // -------- Handlers --------

    /**
     * POST /api/debug/init
     * Body JSON: { degree: number, inputs: number[], function?: string }
     * Returns: { debugId: string, state: DebugStateDTO }
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

        try {
            DebugAPI dbg = target.debugForDegree(degree);
            DebugStateDTO state = dbg.init(execReq);

            String id = UUID.randomUUID().toString();
            getSessions().put(id, dbg);
            getServletContext().setAttribute(ATTR_DBG_BUSY, Boolean.TRUE);

            JsonObject out = new JsonObject();
            out.addProperty("debugId", id);
            out.add("state", gson.toJsonTree(state));
            writeJson(resp, HttpServletResponse.SC_OK, out);
        } catch (Exception e) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Debug init failed: " + e.getMessage());
        }
    }

    /** POST /api/debug/step  (body או query: debugId) — מחזיר DebugStepDTO */
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

            // ריצה עד סיום (ללא תקרה)
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
            // אם אין סשן – מבחינת הלקוח זה "נגמר"
            term = true;
        } else {
            boolean t = false;
            try { t = dbg.isTerminated(); } catch (Throwable ignore) { /* שקט */ }
            term = t;
            if (term) {
                // ניקוי כמו ב-step/resume
                getSessions().remove(debugId);
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("terminated", term);
        writeJson(resp, HttpServletResponse.SC_OK, out);
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
