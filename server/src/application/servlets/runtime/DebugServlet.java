package application.servlets.runtime;

import api.DebugAPI;
import api.DisplayAPI;
import application.credits.Generation;
import application.listeners.AppContextListener;
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
import jakarta.servlet.http.HttpSession;
import users.UserManager;
import users.UserTableRow;

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
        // Parse JSON body
        JsonObject in = readJson(req);
        if (in == null) in = new JsonObject();

        // Which program/function did the client ask to debug?
        String programKey = null;
        if (in.has("program") && !in.get("program").isJsonNull()) {
            programKey = in.get("program").getAsString();
        }

        String functionKey = null;
        if (in.has("function") && !in.get("function").isJsonNull()) {
            functionKey = in.get("function").getAsString();
        }

        // Build the DTO for execution parameters (degree / inputs / generation)
        ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);
        if (execReq == null) {
            writeJsonError(resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Missing execution request body.");
            return;
        }
        int degree = Math.max(0, execReq.getDegree());
        // Resolve which DisplayAPI to debug, based on registry + user selection
        DisplayAPI target = resolveTargetFromRegistry(programKey, functionKey, resp);
        if (target == null) {
            // resolveTargetFromRegistry already wrote the error response
            return;
        }

        // Identify which logged-in user is starting this debug session
        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }

        UserManager um = AppContextListener.getUsers(getServletContext());
        // Charge architecture (generation) up-front for DEBUG init
        int creditsNowAfterInit = 0;
        try {
            Generation gen = Generation.valueOf(execReq.getGeneration());

            if (username != null) {
                // TODO(credits): if not enough credits to afford gen.getCredits(),
                // return an error instead of going forward.
                um.adjustCredits(username, -gen.getCredits());

                UserTableRow row = um.get(username);
                if (row != null) {
                    creditsNowAfterInit = row.getCreditsCurrent();
                }
            }
        } catch (Exception ignore) {
            // If parsing generation fails or user is null we just continue.
            // TODO(credits): consider refund / rollback on failure later.
        }

        // We pre-generate a debug session id for the client
        String id = UUID.randomUUID().toString();

        // Submit heavy work (building the actual DebugAPI and init state) to the executor
        JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugAPI dbg = target.debugForDegree(degree);

                DebugStateDTO state = dbg.init(execReq);

                // Store session data for ongoing debug:
                // - getSessions(): debugId -> DebugAPI (engine handle)
                // - getLocks():    debugId -> Semaphore    (to serialize step/resume)
                // - getSnapshots():debugId -> DebugStateDTO (last known machine state)
                getSessions().put(id, dbg);
                getLocks().computeIfAbsent(id, k -> new Semaphore(1));
                getSnapshots().put(id, state);

                // Mark system "busy" = true if at least one debug session is active
                getServletContext().setAttribute(ATTR_DBG_BUSY, Boolean.TRUE);

                // TODO(history):
                // Here we can also stash per-session metadata (user, programKey, etc.)
                // so that later when this session ends we can bump run counters safely.
                // We'll add that in the history/count stage.

            } catch (Throwable t) {
                // Rollback partial session data if init failed
                getSessions().remove(id);
                getLocks().remove(id);
                getSnapshots().remove(id);

                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(
                        ATTR_DBG_BUSY,
                        anyLeft ? Boolean.TRUE : Boolean.FALSE
                );
                throw t;
            }
            return null;
        });

        // If thread-pool was too busy we tell the client "try again"
        if (!res.isAccepted()) {
            int retryMs = res.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);

            resp.setHeader("Retry-After", String.valueOf(retrySec));

            JsonObject outBusy = new JsonObject();
            outBusy.addProperty("error", "busy");
            outBusy.addProperty("retryMs", retryMs);

            // NOTE: we keep 429 here like קודם (Too Many Requests)
            writeJson(resp, SC_TOO_MANY_REQUESTS, outBusy);
            return;
        }

        // Success: return debugId + credits after upfront generation billing
        JsonObject out = new JsonObject();
        out.addProperty("debugId", id);
        out.addProperty("creditsCurrent", creditsNowAfterInit);
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

        // Identify which logged-in user is doing this debug step.
        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }

        UserManager um = AppContextListener.getUsers(getServletContext());

        // Ensure only one STEP/RESUME runs at a time for this debugId
        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        try {
            // === 1) cycles BEFORE the step ===
            long prevCycles = 0L;
            DebugStateDTO beforeSnap = getSnapshots().get(debugId);
            if (beforeSnap != null) {
                prevCycles = beforeSnap.getCyclesSoFar();
            }

            // === 2) advance one "step over" in the debug engine ===
            DebugStepDTO step = dbg.step();

            // === 3) save latest machine snapshot for /api/debug/state ===
            DebugStateDTO afterSnap = null;
            if (step != null) {
                afterSnap = step.getNewState();
                if (afterSnap != null) {
                    getSnapshots().put(debugId, afterSnap);
                }
            }

            // === 4) bill credits for THIS step ===
            // We measure how many cycles were actually executed in this step.
            int creditsCurrent = 0;
            int creditsUsed = 0;

            if (username != null && afterSnap != null) {
                long currCycles = afterSnap.getCyclesSoFar();
                long delta = currCycles - prevCycles;
                if (delta < 0L) {
                    delta = 0L;
                }

                if (delta > 0L) {
                    // TODO(credits):
                    // if user does NOT have enough credits to pay for "delta",
                    // we should:
                    //   1. stop this debug session,
                    //   2. mark reason = "ended due to insufficient credits",
                    //   3. return an error instead of normal step result.
                    //
                    // For now we assume enough credits and just deduct:
                    um.adjustCredits(username, (int)(-delta));
                }

                // after charging, read updated balance
                UserTableRow row = um.get(username);
                if (row != null) {
                    creditsCurrent = row.getCreditsCurrent();
                    creditsUsed = row.getCreditsUsed();
                }
            }

            // === 5) check if the program is done after this step ===
            boolean term = false;
            try {
                term = dbg.isTerminated();
            } catch (Throwable ignore) {
                // tolerate engine weirdness
            }

            // === 6) build JSON response for the client UI ===
            // We are NOT mutating DebugStepDTO.
            // We wrap it in a JsonObject that also reports credits + termination.
            JsonObject root = new JsonObject();

            // "step" field: the raw debug step info (pc, vars, newState...)
            // Gson will serialize the DebugStepDTO as-is.
            Gson gson = new Gson();
            root.add("step", gson.toJsonTree(step));

            // "credits" field: remaining/used AFTER this step
            JsonObject creditsJson = new JsonObject();
            creditsJson.addProperty("current", creditsCurrent);
            creditsJson.addProperty("used", creditsUsed);
            root.add("credits", creditsJson);

            // "terminated" field: true if program cannot continue stepping
            root.addProperty("terminated", term);

            // === 7) write response ===
            // client will use:
            //   - credits.current to refresh Available Credits live
            //   - terminated=true to disable Step Over/Resume buttons
            writeJson(resp, HttpServletResponse.SC_OK, root);

            // === 8) IMPORTANT: do NOT purge the debug session here ===
            // We keep getSessions()/getSnapshots() alive for now.
            //
            // Why:
            // - The client might immediately ask /api/debug/state to render final state.
            // - Resume flow also relies on the session still existing.
            //
            // Cleanup and history will be handled in:
            //   - /api/debug/terminated (end-of-run)
            //   - /api/debug/stop      (manual stop)
            //
            // TODO(history):
            // When 'term == true', we will later log this debug run:
            //   - username
            //   - which program/function ran
            //   - inputs
            //   - total cycles
            //   - total credits burned
            //   - reason = "completed via step over"
            //   - timestamp
            //
            // Also future: if term==true we will probably schedule cleanup.

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

        // Identify current logged-in user for billing
        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }

        UserManager um = AppContextListener.getUsers(getServletContext());

        // Snapshot cycles at the exact moment resume is requested.
        // We will later bill ONLY for cycles that happened after this point.
        DebugStateDTO startSnap = getSnapshots().get(debugId);
        final long resumeStartCycles = (startSnap != null) ? startSnap.getCyclesSoFar() : 0L;

        // We must capture values as final/effectively-final for the lambda below
        final String usernameCaptured = username;
        final UserManager umCaptured = um;

        // Serialize commands for this debugId using a per-session lock
        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            // Someone is already stepping/resuming this same debugId
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        // Hand off the "run-until-done" work to the executor thread-pool
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

                    // Advance one step in the debug engine
                    last = dbg.step();

                    // Update latest snapshot for /api/debug/state so UI can poll and display progress
                    try {
                        if (last != null && last.getNewState() != null) {
                            getSnapshots().put(debugId, last.getNewState());
                        }
                    } catch (Throwable ignore) {
                        // best-effort snapshot update
                    }
                }

                // ===== Aggregated credit charge for RESUME =====
                // We charge ONCE for all cycles executed during this entire resume window.
                try {
                    DebugStateDTO endSnap = getSnapshots().get(debugId);
                    long endCycles = (endSnap != null) ? endSnap.getCyclesSoFar() : resumeStartCycles;
                    long totalDelta = endCycles - resumeStartCycles;
                    if (totalDelta < 0L) {
                        totalDelta = 0L;
                    }

                    if (usernameCaptured != null && totalDelta > 0L) {
                        // TODO(credits): if the user does not have enough credits to cover totalDelta,
                        // we should stop earlier, mark "ended due to insufficient credits",
                        // and return an error instead of silently running to completion.
                        umCaptured.adjustCredits(usernameCaptured, (int) (-totalDelta));
                    }
                } catch (Throwable ignore) {
                    // best-effort charging; do not fail the whole resume flow
                }

                // NOTE:
                // We DO NOT remove the debug session from getSessions() here.
                // We DO NOT clear getSnapshots() here.
                //
                // We leave:
                //   - getSessions().get(debugId) == dbg
                //   - getSnapshots().get(debugId) == final state
                //
                // so that the client can:
                //   1. ask /api/debug/terminated → gets terminated=true
                //   2. call /api/debug/state    → gets the final snapshot
                //
                // Actual teardown is delayed and will happen in handleTerminated(...).

            } catch (Throwable t) {
                // If resume crashes mid-run:
                // We STILL do not purge the session/snapshot here.
                // The client can still inspect /api/debug/state or try /api/debug/terminated.
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

        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }
        UserManager um = AppContextListener.getUsers(getServletContext());

        int creditsNow = 0;
        if (username != null) {
            UserTableRow row = um.get(username);
            if (row != null) {
                creditsNow = row.getCreditsCurrent();
            }
        }

        JsonObject out = new JsonObject();
        out.addProperty("terminated", term);
        out.addProperty("creditsCurrent", creditsNow);
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

    @SuppressWarnings("unchecked")
    private Map<String, DisplayAPI> getDisplayRegistry() {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_REGISTRY);
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, DisplayAPI>) m;
        }

        // First access: create empty registry so we never get null
        Map<String, DisplayAPI> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DISPLAY_REGISTRY, created);
        return created;
    }

    private DisplayAPI resolveTargetFromRegistry(String programKey, String functionKey, HttpServletResponse resp) throws IOException {
        Map<String, DisplayAPI> registry = getDisplayRegistry();
        DisplayAPI target = null;
        if (functionKey != null && !functionKey.isBlank()) {
            // User asked to debug a function directly
            target = registry.get(functionKey);
            if (target == null) {
                writeJsonError(resp,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Function not found: " + functionKey);
                return null;
            }
        } else {
            // User asked to debug the whole program
            if (programKey == null || programKey.isBlank()) {
                writeJsonError(resp,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Missing program");
                return null;
            }
            target = registry.get(programKey);
            if (target == null) {
                writeJsonError(resp,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Program not found: " + programKey);
                return null;
            }
        }

        if (safeDisplay(target) == null) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT,
                    "Selected program/function is not available anymore.");
            return null;
        }

        return target;
    }

}
