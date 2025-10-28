package application.servlets.runtime;

import api.DebugAPI;
import application.credits.Generation;
import application.history.HistoryManager;
import application.listeners.AppContextListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import display.DisplayDTO;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import application.execution.ExecutionTaskManager;
import application.execution.ExecutionTaskManager.Job;
import application.execution.ExecutionTaskManager.Status;
import application.execution.JobSubmitResult;
import types.VarOptionsDTO;
import types.VarRefDTO;
import users.UserManager;

import api.DisplayAPI;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Constants.*;

@WebServlet(name = "ExecuteServlet", urlPatterns = {API_EXECUTE})
public class ExecuteServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {

        resp.setContentType("application/json");
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = req.getReader()) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
            }
            JsonObject in = gson.fromJson(sb.toString(), JsonObject.class);
            if (in == null) {
                in = new JsonObject();
            }

            String programKey = in.has("program") && !in.get("program").isJsonNull()
                    ? in.get("program").getAsString()
                    : null;
            String functionUserString = in.has("function") && !in.get("function").isJsonNull()
                    ? in.get("function").getAsString()
                    : null;

            ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);

            Map<String, DisplayAPI> registry = getDisplayRegistry();

            DisplayAPI target = null;
            if (functionUserString != null && !functionUserString.isBlank()) {
                target = registry.get(functionUserString);
                if (target == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"function not found\"}");
                    return;
                }
            } else if (programKey != null && !programKey.isBlank()) {
                target = registry.get(programKey);
                if (target == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"program not found\"}");
                    return;
                }
            }

            final int degree = Math.max(0, execReq.getDegree());
            final DisplayAPI targetRef = target;
            final ExecutionRequestDTO execReqRef = execReq;

            final String programKeyRef = programKey;
            final String functionUserStringRef = functionUserString;

            final String username = (String) (req.getSession(false) != null
                    ? req.getSession(false).getAttribute(SESSION_USERNAME)
                    : null);

            final UserManager um = AppContextListener.getUsers(getServletContext());
            final HistoryManager hmRef = AppContextListener.getHistory(getServletContext());

            JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {

                // Build DebugAPI for this run
                final DebugAPI dbgApi = targetRef.debugForDegree(degree);

                final Generation gen = Generation.valueOf(execReqRef.getGeneration());
                // TODO(input): if you want fail-fast 400 on bad generation, validate before scheduling.

                // 1) One-time opening charge for the selected generation
                if (username != null) {
                    um.adjustCredits(username, -gen.getCredits());
                    // TODO(credits): define rollback policy on CANCEL/ERROR (whether to refund gen cost).
                }

                // 2) Step program and charge AFTER each command according to actual cycles
                DebugStateDTO state = dbgApi.init(execReqRef);
                long prev = state.getCyclesSoFar();

                DebugStateDTO lastState = state;

                while (!dbgApi.isTerminated()) {
                    DebugStepDTO step = dbgApi.step();
                    DebugStateDTO newState = step.getNewState();
                    long curr = newState.getCyclesSoFar();
                    long delta = Math.max(0L, curr - prev);

                    if (username != null && delta > 0L) {
                        um.adjustCredits(username, (int) -delta); // charge after the command completed
                        // TODO(credits): if not enough credits for this step → abort gracefully and return an error.
                    }

                    prev = curr;
                    lastState = newState;
                }

                // 3) Finalize result
                DisplayDTO executedDisplay = dbgApi.executedDisplaySnapshot();
                ExecutionDTO result = dbgApi.finalizeExecution(execReqRef, executedDisplay);

                // 4) Persist run history entry (mode = EXECUTION)
                try {
                    String targetType = (functionUserStringRef != null && !functionUserStringRef.isBlank())
                            ? "FUNCTION"
                            : "PROGRAM";
                    String targetName = (functionUserStringRef != null && !functionUserStringRef.isBlank())
                            ? functionUserStringRef
                            : programKeyRef;

                    String architectureType = execReqRef.getGeneration();
                    long cyclesCount = lastState.getCyclesSoFar();

                    long finalY = 0L;
                    finalY=lastState.getY();
                    List<Long> inputsList = execReqRef.getInputs();
                    List<String> outputsSnapshot = buildOutputsSnapshot(lastState);

                    if (username != null && hmRef != null) {
                        hmRef.addRunRecord(
                                username,
                                targetType,
                                targetName,
                                architectureType,
                                degree,
                                finalY,
                                cyclesCount,
                                inputsList,
                                outputsSnapshot,
                                "EXECUTION"
                        );
                    }
                } catch (Exception ignore) {
                    // best-effort only, history failure should not kill the run
                }

                // 5) Optional aggregate metrics (kept from previous code)
                if (username != null) {
                    um.onRunExecuted(username, 0);
                    // TODO(history): when implementing per-user run history, persist full record (inputs, y, cycles, generation, timestamp).
                }

                return result;
            });

            if (!res.isAccepted()) {
                resp.setStatus(SC_TOO_MANY_REQUESTS); // 429
                int retrySec = (int) Math.ceil(res.getRetryAfterMs() / 1000.0);
                resp.setHeader("Retry-After", String.valueOf(retrySec));
                resp.getWriter().write("{\"error\":\"busy\",\"retryMs\":" + res.getRetryAfterMs() + "}");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_ACCEPTED); // 202
            resp.getWriter().write("{\"jobId\":\"" + res.getJobId() + "\"}");

        } catch (Exception ex) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json");
        try {
            String jobId = req.getParameter("jobId");
            if (jobId == null || jobId.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"status\":\"UNKNOWN\",\"error\":\"missing jobId\"}");
                return;
            }

            Job job = ExecutionTaskManager.get(jobId);
            if (job == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"status\":\"UNKNOWN\",\"error\":\"no such job\"}");
                return;
            }

            Status st = job.status;
            if (st == Status.DONE) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\":\"DONE\",\"result\":" + gson.toJson(job.result) + "}");
                return;
            }
            if (st == Status.ERROR) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\":\"ERROR\",\"error\":" + gson.toJson(job.error) + "}");
                return;
            }
            if (st == Status.CANCELED) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\":\"CANCELED\"}");
                return;
            }
            if (st == Status.TIMED_OUT) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\":\"TIMED_OUT\"}");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("{\"status\":\"" + st + "\"}");

        } catch (Exception ex) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
        resp.setContentType("application/json; charset=UTF-8");
        String jobId = req.getParameter("jobId");
        if (jobId == null || jobId.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\":\"UNKNOWN\",\"error\":\"missing jobId\"}");
            return;
        }

        ExecutionTaskManager.Job job = ExecutionTaskManager.get(jobId);
        if (job == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"status\":\"UNKNOWN\",\"error\":\"no such job\"}");
            return;
        }

        job.cancel(); // future.cancel(true) + status=CANCELED
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"status\":\"CANCELED\"}");
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

    /**
     * Build a readable snapshot of final variable values in the order:
     * y, then all x..., then all z...
     */
    private static List<String> buildOutputsSnapshot(DebugStateDTO state) {
        List<String> lines = new ArrayList<>();

        if (state == null || state.getVars() == null) {
            lines.add("y = 0");
            return lines;
        }

        long yVal = 0L;
        Map<String, Long> valuesByName = new HashMap<>();
        Set<Integer> xSet = new TreeSet<>();
        Set<Integer> zSet = new TreeSet<>();

        for (VarValueDTO vv : state.getVars()) {
            VarRefDTO ref = vv.getVar();
            if (ref == null) {
                continue;
            }
            VarOptionsDTO kind = ref.getVariable();
            int idx = ref.getIndex();

            switch (kind) {
                case y:
                    yVal = vv.getValue();
                    break;
                case x:
                    if (idx > 0) {
                        xSet.add(idx);
                        valuesByName.put("x" + idx, vv.getValue());
                    }
                    break;
                case z:
                    if (idx > 0) {
                        zSet.add(idx);
                        valuesByName.put("z" + idx, vv.getValue());
                    }
                    break;
            }
        }
        lines.add("y = " + yVal);
        for (Integer xi : xSet) {
            Long v = valuesByName.get("x" + xi);
            long val = (v == null ? 0L : v);
            lines.add("x" + xi + " = " + val);
        }
        for (Integer zi : zSet) {
            Long v = valuesByName.get("z" + zi);
            long val = (v == null ? 0L : v);
            lines.add("z" + zi + " = " + val);
        }
        return lines;
    }
}
