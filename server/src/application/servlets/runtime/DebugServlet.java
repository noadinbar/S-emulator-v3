package application.servlets.runtime;

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
import java.util.concurrent.Semaphore;

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
                API_DEBUG_HISTORY,
                API_DEBUG_STATE
        }
)
public class DebugServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String route = req.getServletPath(); // full mapping
        switch (route) {
            case API_DEBUG_INIT       -> handleInit(req, resp);
            case API_DEBUG_STEP       -> handleStep(req, resp);
            case API_DEBUG_RESUME     -> handleResume(req, resp);
            case API_DEBUG_STOP       -> handleStop(req, resp);
            case API_DEBUG_TERMINATED -> handleTerminated(req, resp);
            case API_DEBUG_HISTORY    -> handleHistory(req, resp);
            default -> writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                    "Unknown debug route: " + route);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String route = req.getServletPath();
        if (API_DEBUG_STATE.equals(route)) {
            handleState(req, resp);
            return;
        }
        writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                "Unknown debug GET route: " + route);
    }

    // -------- Handlers --------

    /**
     * POST /api/debug/init
     * Body: { degree: number, inputs: number[], function?: string }
     * Async behavior: returns 202 {debugId} or 429 {retryMs}.
     * Heavy work (build DebugAPI + dbg.init) runs on the thread-pool via trySubmit.
     * On success: stores session and initial snapshot (DebugStateDTO).
     */
    private void handleInit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DisplayAPI root = getRootOrError(resp);
        if (root == null) return;

        JsonObject in = readJson(req);
        if (in == null) in = new JsonObject();

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

        // Pre-generate debug session id for client polling upon acceptance.
        String id = UUID.randomUUID().toString();

        JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugAPI dbg = target.debugForDegree(degree);
                DebugStateDTO state = dbg.init(execReq);

                // Store session and per-session lock
                getSessions().put(id, dbg);
                getLocks().computeIfAbsent(id, k -> new Semaphore(1));
                // Store initial snapshot for GET /state
                getSnapshots().put(id, state);

                // Mark "busy" if at least one session exists
                getServletContext().setAttribute(ATTR_DBG_BUSY, Boolean.TRUE);

            } catch (Throwable t) {
                // Clean up on failure
                getSessions().remove(id);
                getLocks().remove(id);
                getSnapshots().remove(id);
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
                throw t;
            }
            return null; // ExecutionTaskManager result not used for debug flow
        });

        if (!res.isAccepted()) {
            int retryMs = res.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);
            resp.setHeader("Retry-After", String.valueOf(retrySec));

            JsonObject out = new JsonObject();
            out.addProperty("error", "busy");
            out.addProperty("retryMs", retryMs);
            writeJson(resp, SC_TOO_MANY_REQUESTS, out);
            return;
        }

        JsonObject out = new JsonObject();
        out.addProperty("debugId", id);
        writeJson(resp, HttpServletResponse.SC_ACCEPTED, out);
    }

    /**
     * POST /api/debug/step (query/body: debugId) — synchronous, serialized per session.
     * Returns DebugStepDTO and updates the latest snapshot to step.getNewState().
     */
    private void handleStep(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = readDebugId(req);
        if (debugId == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().get(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        // Per-session mutex: only one STEP/RESUME at a time for this debugId
        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        try {
            // Advance exactly one logical step in the debugged program
            DebugStepDTO step = dbg.step();

            // Save latest machine state snapshot so /api/debug/state can return it
            try {
                if (step != null && step.getNewState() != null) {
                    getSnapshots().put(debugId, step.getNewState());
                }
            } catch (Throwable ignore) {
                // best-effort snapshot update; debugger should not crash if UI can't cache
            }

            // DO NOT clean up here, even if dbg.isTerminated() == true.
            writeJson(resp, HttpServletResponse.SC_OK, step);

        } catch (Exception e) {
            writeJsonError(
                    resp,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Debug step failed: " + e.getMessage()
            );
        } finally {
            lock.release();
        }
    }


    /**
     * POST /api/debug/resume (query/body: debugId)
     */
    private void handleResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = readDebugId(req);
        if (debugId == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().get(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        // Serialize commands for this debugId
        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            // Someone is already stepping/resuming this same debugId
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        // Hand off the long "run until done" work to the executor thread-pool
        JobSubmitResult submitResult = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugStepDTO last = null;

                // Keep stepping until program reports isTerminated() OR thread interrupted
                while (true) {
                    boolean alreadyDone = false;
                    try {
                        alreadyDone = dbg.isTerminated();
                    } catch (Throwable ignore) {
                        // tolerate weird debug engine states
                    }
                    if (alreadyDone) {
                        break;
                    }

                    // If executor canceled us (timeout / shutdown), exit gracefully
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    // Advance execution
                    last = dbg.step();

                    // Update latest snapshot for /api/debug/state
                    try {
                        if (last != null && last.getNewState() != null) {
                            getSnapshots().put(debugId, last.getNewState());
                        }
                    } catch (Throwable ignore) {
                        // best-effort snapshot update
                    }
                }

                // NOTE:
                // We DO NOT call getSessions().remove(...) here.
                // We DO NOT clear getSnapshots() here.
                //
                // We intentionally leave:
                //   - getSessions().get(debugId) == dbg
                //   - getSnapshots().get(debugId) == final state
                //
                // So the client can:
                //   1. Ask /api/debug/terminated → gets terminated=true
                //   2. Immediately call /api/debug/state  → gets the final snapshot
                //
                // Actual teardown will happen later (delayed) in handleTerminated(...).

            } catch (Throwable t) {
                // If resume crashes mid-run:
                // We STILL do not purge the session/snapshot here.
                // Client can inspect /api/debug/state or try /api/debug/terminated.
                throw t;
            } finally {
                // Always release and drop the per-session lock so this debugId is no longer "actively running".
                try {
                    lock.release();
                } catch (Throwable ignore) {
                    // swallow
                }
                getLocks().remove(debugId);
            }
            return null;
        });

        if (!submitResult.isAccepted()) {
            // Executor is overloaded. Tell the client to retry later.
            int retryMs = submitResult.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);
            resp.setHeader("Retry-After", String.valueOf(retrySec));

            // Since job was NOT accepted, we must free the lock now.
            try {
                lock.release();
            } catch (Throwable ignore) {
                // swallow
            }
            getLocks().remove(debugId);

            JsonObject out = new JsonObject();
            out.addProperty("error", "busy");
            out.addProperty("retryMs", retryMs);
            writeJson(resp, SC_TOO_MANY_REQUESTS, out);
            return;
        }

        // Job accepted (202): client will now poll /api/debug/state and /api/debug/terminated
        JsonObject out = new JsonObject();
        out.addProperty("debugId", debugId);
        writeJson(resp, HttpServletResponse.SC_ACCEPTED, out);
    }

    /** POST /api/debug/stop — stops a specific session (by debugId) and clears snapshot. */
    private void handleStop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = readDebugId(req);
        if (debugId == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugAPI dbg = getSessions().remove(debugId);
        if (dbg == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
            return;
        }

        // Remove lock and snapshot for this session
        getLocks().remove(debugId);
        getSnapshots().remove(debugId);

        boolean anyLeft = !getSessions().isEmpty();
        getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);

        JsonObject out = new JsonObject();
        out.addProperty("stopped", true);
        out.addProperty("debugId", debugId);
        out.addProperty("remaining", getSessions().size());
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }

    /**
     * POST /api/debug/terminated (query/body: debugId)
     */
    private void handleTerminated(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = readDebugId(req);
        if (debugId == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        boolean term;
        DebugAPI dbg = getSessions().get(debugId);

        if (dbg == null) {
            // Session already cleaned up earlier (e.g. user pressed Stop).
            // From the client's POV, it's definitely done.
            term = true;
        } else {
            boolean done = false;
            try {
                done = dbg.isTerminated();
            } catch (Throwable ignore) {
                // tolerate any weird internal engine state
            }
            term = done;

            if (term) {
                // Program really is finished now.
                // We still KEEP the snapshot and session for a tiny grace window,
                // so the client can immediately fetch /api/debug/state.
                //
                // After that short grace window, we'll wipe everything using
                // cleanupDebugSessionSoon(debugId), which:
                //   - removes session / snapshot / lock from the maps
                //   - updates ATTR_DBG_BUSY to reflect no active debug
                cleanupDebugSessionSoon(debugId);
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("terminated", term);
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }


    /** GET /api/debug/state?debugId=... → { debugId, state: DebugStateDTO } or 204/404 */
    private void handleState(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String debugId = req.getParameter("debugId");
        if (debugId == null || debugId.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing debugId");
            return;
        }

        DebugStateDTO snap = getSnapshots().get(debugId);
        if (snap != null) {
            JsonObject out = new JsonObject();
            out.addProperty("debugId", debugId);
            out.add("state", gson.toJsonTree(snap));
            writeJson(resp, HttpServletResponse.SC_OK, out);
            return;
        }

        // No snapshot available yet:
        // If session exists, return 204 (no content yet). If session does not exist, return 404.
        if (getSessions().containsKey(debugId)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
        } else {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Semaphore> getLocks() {
        Object obj = getServletContext().getAttribute(ATTR_DEBUG_LOCKS);
        if (obj instanceof Map<?, ?> m) return (Map<String, Semaphore>) m;
        Map<String, Semaphore> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DEBUG_LOCKS, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private Map<String, DebugStateDTO> getSnapshots() {
        Object obj = getServletContext().getAttribute(ATTR_DEBUG_SNAPSHOTS);
        if (obj instanceof Map<?, ?> m) return (Map<String, DebugStateDTO>) m;
        Map<String, DebugStateDTO> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DEBUG_SNAPSHOTS, created);
        return created;
    }

    private String readDebugId(HttpServletRequest req) throws IOException {
        String debugId = req.getParameter("debugId");
        if (debugId == null || debugId.isBlank()) {
            JsonObject in = readJson(req);
            if (in != null && in.has("debugId") && !in.get("debugId").isJsonNull()) {
                debugId = in.get("debugId").getAsString();
            }
        }
        return (debugId == null || debugId.isBlank()) ? null : debugId;
    }

    private JsonObject readJson(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        if (sb.isEmpty()) return null;
        return gson.fromJson(sb.toString(), JsonObject.class);
    }

    /**
     * Gracefully destroy a finished debug session after a short delay.
     */
    private void cleanupDebugSessionSoon(final String debugId) {
        Thread cleaner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Small grace period so the client can still read /api/debug/state
                    Thread.sleep(500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                // Remove the debug session, its lock, and its last snapshot
                getSessions().remove(debugId);
                getLocks().remove(debugId);
                getSnapshots().remove(debugId);

                // Update "busy" flag: if there are no more sessions, dbgBusy=false
                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
            }
        }, "debug-cleanup-" + debugId);

        cleaner.setDaemon(true); // does not block server shutdown
        cleaner.start();
    }

}
