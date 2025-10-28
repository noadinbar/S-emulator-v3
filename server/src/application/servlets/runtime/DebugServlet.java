package application.servlets.runtime;

import api.DebugAPI;
import api.DisplayAPI;
import application.credits.Generation;
import application.history.HistoryManager;
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
import jakarta.servlet.http.HttpSession;
import users.UserManager;
import users.UserTableRow;
import application.execution.ExecutionTaskManager;
import application.execution.JobSubmitResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static utils.Constants.*;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "DebugServlet", urlPatterns = { API_DEBUG_INIT, API_DEBUG_STEP, API_DEBUG_RESUME, API_DEBUG_STOP, API_DEBUG_TERMINATED, API_DEBUG_HISTORY, API_DEBUG_STATE})
public class DebugServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String route = req.getServletPath();
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
     * Body: { degree: number, inputs: number[], program?: string, function?: string, generation: "I"/... }
     * Returns 202 {debugId, creditsCurrent} or 429 busy.
     * Creates the debug session (engine handle + snapshot + lock + meta).
     */
    private void handleInit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject in = readJson(req);
        if (in == null) {
            in = new JsonObject();
        }

        String programKey = null;
        if (in.has("program") && !in.get("program").isJsonNull()) {
            programKey = in.get("program").getAsString();
        }

        String functionKey = null;
        if (in.has("function") && !in.get("function").isJsonNull()) {
            functionKey = in.get("function").getAsString();
        }

        ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);
        if (execReq == null) {
            writeJsonError(resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Missing execution request body.");
            return;
        }

        int degree = Math.max(0, execReq.getDegree());

        DisplayAPI target = resolveTargetFromRegistry(programKey, functionKey, resp);
        if (target == null) {
            return;
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
            // TODO(credits): consider refund / rollback on failure later.
        }

        String id = UUID.randomUUID().toString();

        final String usernameRef = username;
        final int degreeRef = degree;
        final ExecutionRequestDTO execReqRef = execReq;
        final String programKeyRef = programKey;
        final String functionKeyRef = functionKey;
        final DisplayAPI targetRef = target;

        JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugAPI dbg = targetRef.debugForDegree(degreeRef);
                DebugStateDTO state = dbg.init(execReqRef);

                getSessions().put(id, dbg);
                getLocks().computeIfAbsent(id, k -> new Semaphore(1));
                getSnapshots().put(id, state);

                getServletContext().setAttribute(ATTR_DBG_BUSY, Boolean.TRUE);

                // Store meta for this debug session so we can later write it to history.
                String targetType = (functionKeyRef != null && !functionKeyRef.isBlank())
                        ? "FUNCTION"
                        : "PROGRAM";
                String targetName = (functionKeyRef != null && !functionKeyRef.isBlank())
                        ? functionKeyRef
                        : programKeyRef;

                DebugSessionMeta meta = new DebugSessionMeta(
                        usernameRef,
                        targetType,
                        targetName,
                        execReqRef.getGeneration(),
                        degreeRef,
                        execReqRef.getInputs()
                );

                getDebugMetas().put(id, meta);

                // TODO(history): when user clicks "back to opening" without calling /api/debug/stop,
                // the UI should still call a dedicated endpoint to finalize and persist history.

            } catch (Throwable t) {
                getSessions().remove(id);
                getLocks().remove(id);
                getSnapshots().remove(id);
                getDebugMetas().remove(id);

                boolean anyLeft = !getSessions().isEmpty();
                getServletContext().setAttribute(
                        ATTR_DBG_BUSY,
                        anyLeft ? Boolean.TRUE : Boolean.FALSE
                );
                throw t;
            }
            return null;
        });

        if (!res.isAccepted()) {
            int retryMs = res.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);

            resp.setHeader("Retry-After", String.valueOf(retrySec));

            JsonObject outBusy = new JsonObject();
            outBusy.addProperty("error", "busy");
            outBusy.addProperty("retryMs", retryMs);

            writeJson(resp, SC_TOO_MANY_REQUESTS, outBusy);
            return;
        }

        JsonObject out = new JsonObject();
        out.addProperty("debugId", id);
        out.addProperty("creditsCurrent", creditsNowAfterInit);
        writeJson(resp, HttpServletResponse.SC_ACCEPTED, out);
    }

    /**
     * POST /api/debug/step
     * Body or param: debugId
     * Charges credits for this single step, advances state, returns DebugStepDTO + termination flag.
     * If after this step the program reached EXIT (terminated=true), we write history.
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

        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }

        UserManager um = AppContextListener.getUsers(getServletContext());

        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        try {
            long prevCycles = 0L;
            DebugStateDTO beforeSnap = getSnapshots().get(debugId);
            if (beforeSnap != null) {
                prevCycles = beforeSnap.getCyclesSoFar();
            }

            DebugStepDTO step = dbg.step();

            DebugStateDTO afterSnap = null;
            if (step != null) {
                afterSnap = step.getNewState();
                if (afterSnap != null) {
                    getSnapshots().put(debugId, afterSnap);
                }
            }

            int creditsCurrent = 0;
            int creditsUsed = 0;

            if (username != null && afterSnap != null) {
                long currCycles = afterSnap.getCyclesSoFar();
                long delta = currCycles - prevCycles;
                if (delta < 0L) {
                    delta = 0L;
                }

                if (delta > 0L) {
                    // TODO(credits): if user does NOT have enough credits to pay for this step,
                    // persist partial history (ended due to insufficient credits) and close session.
                    um.adjustCredits(username, (int)(-delta));
                }

                UserTableRow row = um.get(username);
                if (row != null) {
                    creditsCurrent = row.getCreditsCurrent();
                    creditsUsed = row.getCreditsUsed();
                }
            }

            boolean term = false;
            try {
                term = dbg.isTerminated();
            } catch (Throwable ignore) { }

            // if program just finished because of this step -> write debug history now
            if (term) {
                saveHistoryForDebugSession(debugId, afterSnap);
                // we do NOT clean up here so the client can still call /api/debug/state
                // cleanup will still happen after /api/debug/terminated
            }

            JsonObject root = new JsonObject();

            root.add("step", gson.toJsonTree(step));

            JsonObject creditsJson = new JsonObject();
            creditsJson.addProperty("current", creditsCurrent);
            creditsJson.addProperty("used", creditsUsed);
            root.add("credits", creditsJson);

            root.addProperty("terminated", term);

            writeJson(resp, HttpServletResponse.SC_OK, root);

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
     * POST /api/debug/resume
     * Body or param: debugId
     * Runs steps in a loop until program terminates or the worker thread is interrupted.
     * Charges credits once for the full resume window.
     * After finishing, persists debug history.
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

        HttpSession httpSess = req.getSession(false);
        String username = null;
        if (httpSess != null) {
            Object u = httpSess.getAttribute(SESSION_USERNAME);
            if (u instanceof String) {
                username = (String) u;
            }
        }

        UserManager um = AppContextListener.getUsers(getServletContext());

        DebugStateDTO startSnap = getSnapshots().get(debugId);
        final long resumeStartCycles = (startSnap != null) ? startSnap.getCyclesSoFar() : 0L;

        final String usernameCaptured = username;
        final UserManager umCaptured = um;
        final String debugIdRef = debugId;

        Semaphore lock = getLocks().computeIfAbsent(debugId, k -> new Semaphore(1));
        if (!lock.tryAcquire()) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "busy");
            return;
        }

        JobSubmitResult submitResult = ExecutionTaskManager.trySubmit(() -> {
            try {
                DebugStepDTO last;

                while (true) {
                    boolean alreadyDone = false;
                    try {
                        alreadyDone = dbg.isTerminated();
                    } catch (Throwable ignore) { }
                    if (alreadyDone) {
                        break;
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    last = dbg.step();

                    try {
                        if (last != null && last.getNewState() != null) {
                            getSnapshots().put(debugIdRef, last.getNewState());
                        }
                    } catch (Throwable ignore) { }
                }

                // aggregated billing for resume
                try {
                    DebugStateDTO endSnap = getSnapshots().get(debugIdRef);
                    long endCycles = (endSnap != null) ? endSnap.getCyclesSoFar() : resumeStartCycles;
                    long totalDelta = endCycles - resumeStartCycles;
                    if (totalDelta < 0L) {
                        totalDelta = 0L;
                    }

                    if (usernameCaptured != null && totalDelta > 0L) {
                        // TODO(credits): if user does not have enough credits to cover totalDelta,
                        // persist partial history (ended due to insufficient credits) and close session.
                        umCaptured.adjustCredits(usernameCaptured, (int) (-totalDelta));
                    }

                    // write debug history at the end of resume
                    saveHistoryForDebugSession(debugIdRef, endSnap);

                } catch (Throwable ignore) {
                    // best-effort billing + history. Do not crash resume if this fails.
                }

            } finally {
                try {
                    lock.release();
                } catch (Throwable ignore) { }
                getLocks().remove(debugIdRef);
            }

            return null;
        });

        if (!submitResult.isAccepted()) {
            int retryMs = submitResult.getRetryAfterMs();
            int retrySec = (int) Math.ceil(retryMs / 1000.0);
            resp.setHeader("Retry-After", String.valueOf(retrySec));

            try {
                lock.release();
            } catch (Throwable ignore) { }
            getLocks().remove(debugIdRef);

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

    /**
     * POST /api/debug/stop
     * Body or param: debugId
     * User explicitly stopped debugging.
     * We persist debug history now (mode="DEBUG") and then clean up the session.
     * This also covers the "back to opening" flow, assuming the client calls /api/debug/stop.
     *
     * TODO(history): if the UI can navigate away without calling /api/debug/stop,
     * add a dedicated endpoint and call saveHistoryForDebugSession(...) there too.
     */
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

        DebugStateDTO snap = getSnapshots().get(debugId);

        // write history before tearing down
        saveHistoryForDebugSession(debugId, snap);

        // full cleanup
        getLocks().remove(debugId);
        getSnapshots().remove(debugId);
        getDebugMetas().remove(debugId);

        boolean anyLeft = !getSessions().isEmpty();
        getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);

        JsonObject out = new JsonObject();
        out.addProperty("stopped", true);
        out.addProperty("debugId", debugId);
        out.addProperty("remaining", getSessions().size());
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }

    /**
     * POST /api/debug/terminated
     * Body or param: debugId
     * Client asks "are we done" and also wants the final credits.
     * If terminated=true we persist debug history (if not already persisted),
     * and schedule async cleanup (with grace so the client can still pull /api/debug/state).
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
            // Session already cleaned up (for example after stop)
            term = true;
        } else {
            boolean done = false;
            try {
                done = dbg.isTerminated();
            } catch (Throwable ignore) { }
            term = done;

            if (term) {
                DebugStateDTO snap = getSnapshots().get(debugId);

                // persist debug history (if not already persisted)
                saveHistoryForDebugSession(debugId, snap);

                // cleanup will run after a short grace window
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

    /**
     * GET /api/debug/state?debugId=...
     * Returns latest snapshot for that debug session.
     */
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

        if (getSessions().containsKey(debugId)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
        } else {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown debugId");
        }
    }

    /**
     * This is legacy handler left from previous stages.
     * Keeping it as-is.
     */
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

    // -------- History persistence helper --------

    /**
     * Persist this debug session into HistoryManager (mode="DEBUG"), once per session.
     * Uses DebugSessionMeta + latest DebugStateDTO snapshot.
     * Also marks the session meta as recorded to avoid duplicates.
     */
    private void saveHistoryForDebugSession(String debugId, DebugStateDTO finalSnap) {
        DebugSessionMeta meta = getDebugMetas().get(debugId);
        if (meta == null) {
            return;
        }

        synchronized (meta) {
            if (meta.isRecorded()) {
                return;
            }

            HistoryManager hmLocal = AppContextListener.getHistory(getServletContext());
            if (hmLocal == null) {
                return;
            }

            String username = meta.getUsername();
            if (username == null || username.isBlank()) {
                return;
            }

            String targetType = meta.getTargetType();
            String targetName = meta.getTargetName();
            String architectureType = meta.getArchitectureType();
            int degree = meta.getDegree();

            long cyclesCount = (finalSnap != null) ? finalSnap.getCyclesSoFar() : 0L;

            long finalY = 0L;
            if (finalSnap != null) {
                finalY = finalSnap.getY();
            }

            List<Long> inputsList = meta.getInputs();

            List<String> outputsSnapshot = new ArrayList<>();
            if (finalSnap != null) {
                // keep a serialized view of the last known machine state
                outputsSnapshot.add(gson.toJson(finalSnap));
            }

            hmLocal.addRunRecord(
                    username,
                    targetType,
                    targetName,
                    architectureType,
                    degree,
                    finalY,
                    cyclesCount,
                    inputsList,
                    outputsSnapshot,
                    "DEBUG"
            );
            meta.markRecorded();
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

    @SuppressWarnings("unchecked")
    private Map<String, DebugSessionMeta> getDebugMetas() {
        Object obj = getServletContext().getAttribute(ATTR_DEBUG_META);
        if (obj instanceof Map<?, ?> m) return (Map<String, DebugSessionMeta>) m;
        Map<String, DebugSessionMeta> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DEBUG_META, created);
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
     * After a short delay, clean up all maps for this debugId and update busy flag.
     * We also remove meta here to avoid leaks.
     */
    private void cleanupDebugSessionSoon(final String debugId) {
        Thread cleaner = new Thread(() -> {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            getSessions().remove(debugId);
            getLocks().remove(debugId);
            getSnapshots().remove(debugId);
            getDebugMetas().remove(debugId);

            boolean anyLeft = !getSessions().isEmpty();
            getServletContext().setAttribute(ATTR_DBG_BUSY, anyLeft ? Boolean.TRUE : Boolean.FALSE);
        }, "debug-cleanup-" + debugId);

        cleaner.setDaemon(true);
        cleaner.start();
    }

    @SuppressWarnings("unchecked")
    private Map<String, DisplayAPI> getDisplayRegistry() {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_REGISTRY);
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, DisplayAPI>) m;
        }

        Map<String, DisplayAPI> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DISPLAY_REGISTRY, created);
        return created;
    }

    private DisplayAPI resolveTargetFromRegistry(String programKey, String functionKey, HttpServletResponse resp) throws IOException {
        Map<String, DisplayAPI> registry = getDisplayRegistry();
        DisplayAPI target;
        if (functionKey != null && !functionKey.isBlank()) {
            target = registry.get(functionKey);
            if (target == null) {
                writeJsonError(resp,
                        HttpServletResponse.SC_NOT_FOUND,
                        "Function not found: " + functionKey);
                return null;
            }
        } else {
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