package application.servlets;

import application.execution.ExecutionCache;
import application.execution.ProgramLocks;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import api.DisplayAPI;
import api.ExecutionAPI;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

import application.execution.ExecutionTaskManager;
import application.execution.ExecutionTaskManager.Job;
import application.execution.ExecutionTaskManager.Status;
import application.execution.JobSubmitResult;

import java.io.BufferedReader;
import java.util.concurrent.locks.ReadWriteLock;

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
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JsonObject in = gson.fromJson(sb.toString(), JsonObject.class);
            if (in == null) in = new JsonObject();

            String functionUserString = in.has("function") && !in.get("function").isJsonNull()
                    ? in.get("function").getAsString()
                    : null;

            ExecutionRequestDTO execReq = gson.fromJson(in, ExecutionRequestDTO.class);

            DisplayAPI root = (DisplayAPI) getServletContext().getAttribute(ATTR_DISPLAY_API);
            if (root == null || root.getDisplay() == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"no program loaded\"}");
                return;
            }

            DisplayAPI target = root;
            if (functionUserString != null && !functionUserString.isBlank()) {
                DisplayAPI f = root.functionDisplaysByUserString().get(functionUserString);
                if (f == null) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"function not found\"}");
                    return;
                }
                target = f;
            }

            final int degree = Math.max(0, execReq.getDegree());
            final DisplayAPI targetRef = target;
            final ExecutionRequestDTO execReqRef = execReq;
            final ReadWriteLock rw = ProgramLocks.lockFor("REPO");

            // ===== הגשה אסינכרונית עם ויסות עומס (BUSY → 429) =====
            JobSubmitResult res = ExecutionTaskManager.trySubmit(() -> {
                final ExecutionAPI execApi;
                rw.readLock().lock();
                try {
                    execApi = ExecutionCache.getOrCompute(
                            targetRef, degree, () -> targetRef.executionForDegree(degree)
                    );
                } finally {
                    rw.readLock().unlock();
                }
                // הריצה הכבדה – ללא נעילה
                ExecutionDTO result = execApi.execute(execReqRef);
                return result;
            });

            if (!res.isAccepted()) {
                resp.setStatus(SC_TOO_MANY_REQUESTS); // 429
                // רמז ללקוח מתי לנסות שוב (שניות)
                int retrySec = (int) Math.ceil(res.getRetryAfterMs() / 1000.0);
                resp.setHeader("Retry-After", String.valueOf(retrySec));
                resp.getWriter().write("{\"error\":\"busy\",\"retryMs\":" + res.getRetryAfterMs() + "}");
                return;
            }

            // הצלחה: jobId חוזר מיד (לא ממתינים להרצה)
            resp.setStatus(HttpServletResponse.SC_ACCEPTED); // 202
            resp.getWriter().write("{\"jobId\":\"" + res.getJobId() + "\"}");

        } catch (Exception ex) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
            } catch (Exception ignore) {}
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
            } catch (Exception ignore) {}
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
}
