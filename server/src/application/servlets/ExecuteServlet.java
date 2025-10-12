package application.servlets;

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

import java.io.BufferedReader;

import static utils.Constants.API_EXECUTE;
import static utils.Constants.ATTR_DISPLAY_API;

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
            int degree= Math.max(0, execReq.getDegree());
            ExecutionAPI execApi = target.executionForDegree(degree);
            ExecutionDTO result = execApi.execute(execReq);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(gson.toJson(result));
        } catch (Exception ex) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
            } catch (Exception ignore) {}
        }
    }
}
