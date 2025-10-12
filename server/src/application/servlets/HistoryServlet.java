package application.servlets;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

import api.DisplayAPI;
import execution.HistoryDTO;

import static utils.Constants.API_HISTORY;
import static utils.Constants.ATTR_DISPLAY_API;

@WebServlet(name = "HistoryServlet", urlPatterns = {API_HISTORY})
public class HistoryServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        DisplayAPI root = (DisplayAPI) getServletContext().getAttribute(ATTR_DISPLAY_API);
        if (root == null || root.getDisplay() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"no program loaded\"}");
            return;
        }

        String function = req.getParameter("function");
        DisplayAPI target = root;
        if (function != null && !function.isBlank()) {
            Map<String, DisplayAPI> map = root.functionDisplaysByUserString();
            target = (map != null) ? map.get(function) : null;
            if (target == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"function not found\"}");
                return;
            }
        }

        HistoryDTO history = target.getHistory();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(history));
    }
}
