package application.servlets.runtime;

import api.DebugAPI;
import application.credits.Generation;
import application.history.HistoryManager;
import application.listeners.AppContextListener;
import application.programs.ProgramManager;
import application.programs.ProgramTableRow;
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
import users.UserTableRow;

import java.io.BufferedReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Constants.*;

@WebServlet(name = "ExecuteServlet", urlPatterns = {API_EXECUTE})
public class ExecuteServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private Generation architecture;
    boolean outOfCredits = false;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        final long[] totalCycles = new long[1];
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

            // --- Credits gate before scheduling ---
            // Only block main PROGRAM runs (not FUNCTION runs)
            if (username != null
                    && (functionUserString == null || functionUserString.isBlank())
                    && programKey != null
                    && !programKey.isBlank()) {

                ProgramManager pmGate = AppContextListener.getPrograms(getServletContext());
                UserTableRow userRowGate = um.get(username);

                if (pmGate != null && userRowGate != null) {
                    ProgramTableRow progRowGate = pmGate.getRecord(programKey);

                    double avgCost = 0.0;
                    if (progRowGate != null) {
                        avgCost = progRowGate.getAvgCredits();
                    }

                    Generation genGate;
                    try {
                        genGate = Generation.valueOf(execReq.getGeneration());
                    } catch (Exception e) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("{\"error\":\"bad generation\"}");
                        return;
                    }

                    int needCredits = (int) Math.ceil(avgCost);
                    int haveCredits = userRowGate.getCreditsCurrent();

                    if (haveCredits < needCredits) {
                        // Not enough credits to even start
                        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        resp.getWriter().write("{\"error\":\"insufficient_credits\"}");
                        return;
                    }
                }
            }

            JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
                try {
                    // DebugAPI for this specific degree
                    DebugAPI dbgApi = targetRef.debugForDegree(degree);

                    Generation gen = Generation.valueOf(execReqRef.getGeneration());
                    architecture = gen;

                    // 1) upfront charge for the chosen architecture ("generation")
                    if (username != null) {
                        um.adjustCredits(username, -gen.getCredits());
                    }

                    // 2) init machine state
                    DebugStateDTO state = dbgApi.init(execReqRef);
                    long prevCycles = state.getCyclesSoFar();

                    // paidState = last fully-paid state
                    DebugStateDTO paidState = state;

                    while (true) {
                        boolean alreadyTerminated = false;
                        try {
                            alreadyTerminated = dbgApi.isTerminated();
                        } catch (Throwable ignore) { }

                        if (alreadyTerminated) {
                            break;
                        }

                        DebugStepDTO step = dbgApi.step();
                        DebugStateDTO newState = step.getNewState();

                        long currCycles = newState.getCyclesSoFar();
                        long delta = currCycles - prevCycles;
                        if (delta < 0L) {
                            delta = 0L;
                        }

                        if (username != null && delta > 0L) {
                            // check if the user can afford this command
                            UserTableRow rowNow = um.get(username);
                            int haveNow = 0;
                            if (rowNow != null) {
                                haveNow = rowNow.getCreditsCurrent();
                            }

                            if (haveNow < delta) {
                                outOfCredits = true;
                                break;
                            }

                            // charge delta (this command is fully paid)
                            um.adjustCredits(username, (int)(-delta));
                        }

                        prevCycles = currCycles;
                        paidState = newState;
                    }

                    // final snapshot for history / client is the last fully-paid state
                    DebugStateDTO finalStateForHistory = paidState;

                    long finalCycles = finalStateForHistory.getCyclesSoFar();
                    long finalY = finalStateForHistory.getY();
                    List<VarValueDTO> finalVars = finalStateForHistory.getVars();

                    // executedProgram is the expanded program that actually ran
                    DisplayDTO executedDisplay = dbgApi.executedDisplaySnapshot();

                    // Build the DTO we return to the client poll
                    ExecutionDTO result = new ExecutionDTO(
                            finalY,
                            finalCycles,
                            finalVars,
                            executedDisplay
                    );

                    // 3) history record (mode = EXECUTION)
                    try {
                        String targetType = (functionUserStringRef != null && !functionUserStringRef.isBlank())
                                ? "FUNCTION"
                                : "PROGRAM";

                        String targetName = (functionUserStringRef != null && !functionUserStringRef.isBlank())
                                ? functionUserStringRef
                                : programKeyRef;

                        String architectureType = execReqRef.getGeneration();

                        long cyclesCount = finalCycles;
                        totalCycles[0] = cyclesCount;

                        List<Long> inputsList = execReqRef.getInputs();
                        List<String> outputsSnapshot = buildOutputsSnapshot(finalStateForHistory);

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
                        // best-effort only
                    }

                    // 4) per-user aggregate metrics (unchanged)
                    if (username != null) {
                        um.onRunExecuted(username, 0);
                    }

                    // NOTE: we keep outOfCredits in a local boolean.
                    // next step: we will expose it on the Job and return it in doGet().

                    return result;

                } finally {
                    // count this run toward averages for PROGRAM targets
                    if (functionUserStringRef == null || functionUserStringRef.isBlank()) {
                        ProgramManager pm = AppContextListener.getPrograms(getServletContext());
                        if (pm != null && programKeyRef != null && !programKeyRef.isBlank()) {
                            pm.incRunCount(programKeyRef, totalCycles[0]);
                        }
                    }
                }
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

                JsonObject outBad = new JsonObject();
                outBad.addProperty("status", "UNKNOWN");
                outBad.addProperty("error", "missing jobId");
                outBad.addProperty("outOfCredits", false);

                resp.getWriter().write(gson.toJson(outBad));
                return;
            }

            Job job = ExecutionTaskManager.get(jobId);
            if (job == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

                JsonObject outMissing = new JsonObject();
                outMissing.addProperty("status", "UNKNOWN");
                outMissing.addProperty("error", "no such job");
                outMissing.addProperty("outOfCredits", false);

                resp.getWriter().write(gson.toJson(outMissing));
                return;
            }

            boolean outOfCreditsFlag = outOfCredits;
            Status st = job.status;

            if (st == Status.DONE) {
                resp.setStatus(HttpServletResponse.SC_OK);

                JsonObject outDone = new JsonObject();
                outDone.addProperty("status", "DONE");
                outDone.add("result", gson.toJsonTree(job.result));
                outDone.addProperty("outOfCredits", outOfCreditsFlag);

                resp.getWriter().write(gson.toJson(outDone));
                return;
            }

            if (st == Status.ERROR) {
                resp.setStatus(HttpServletResponse.SC_OK);

                JsonObject outErr = new JsonObject();
                outErr.addProperty("status", "ERROR");
                outErr.add("error", gson.toJsonTree(job.error));
                outErr.addProperty("outOfCredits", outOfCreditsFlag);

                resp.getWriter().write(gson.toJson(outErr));
                return;
            }

            if (st == Status.CANCELED) {
                resp.setStatus(HttpServletResponse.SC_OK);

                JsonObject outCanceled = new JsonObject();
                outCanceled.addProperty("status", "CANCELED");
                outCanceled.addProperty("outOfCredits", outOfCreditsFlag);

                resp.getWriter().write(gson.toJson(outCanceled));
                return;
            }

            if (st == Status.TIMED_OUT) {
                resp.setStatus(HttpServletResponse.SC_OK);

                JsonObject outTimeout = new JsonObject();
                outTimeout.addProperty("status", "TIMED_OUT");
                outTimeout.addProperty("outOfCredits", outOfCreditsFlag);

                resp.getWriter().write(gson.toJson(outTimeout));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);

            JsonObject outPending = new JsonObject();
            outPending.addProperty("status", st.toString());
            outPending.addProperty("outOfCredits", outOfCreditsFlag);

            resp.getWriter().write(gson.toJson(outPending));

        } catch (Exception ex) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                JsonObject outFail = new JsonObject();
                outFail.addProperty("error", ex.getMessage());
                outFail.addProperty("outOfCredits", false);

                resp.getWriter().write(gson.toJson(outFail));
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

    private static class RunOutcome {
        // what the client expects normally
        private final ExecutionDTO exec;
        private final boolean outOfCredits;

        RunOutcome(ExecutionDTO exec, boolean outOfCredits) {
            this.exec = exec;
            this.outOfCredits = outOfCredits;
        }

        public ExecutionDTO getExec() {
            return exec;
        }

        public boolean isOutOfCredits() {
            return outOfCredits;
        }
    }

}
