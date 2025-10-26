package application.servlets.runtime;

import api.DisplayAPI;
import display.ExpandDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Constants.API_EXPAND;
import static utils.Constants.ATTR_DISPLAY_REGISTRY;
import static utils.Constants.QP_DEGREE;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "ExpandServlet", urlPatterns = {API_EXPAND})
public class ExpandServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        // which degree is requested
        String degStr = req.getParameter(QP_DEGREE); // "degree"
        int degree = 0;
        try {
            if (degStr != null && !degStr.isBlank()) {
                degree = Math.max(0, Integer.parseInt(degStr));
            }
        } catch (NumberFormatException ignore) { }

        // who are we expanding: program or function
        String functionKey = req.getParameter("function");
        String programKey  = req.getParameter("program");

        Map<String, DisplayAPI> registry = getDisplayRegistry(req);

        DisplayAPI target = null;
        if (functionKey != null && !functionKey.isBlank()) {
            // expand a specific function (function user string is unique)
            target = registry.get(functionKey);
            if (target == null) {
                writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "function not found");
                return;
            }
        } else {
            // expand a whole program (must send program)
            if (programKey == null || programKey.isBlank()) {
                writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "missing program");
                return;
            }
            target = registry.get(programKey);
            if (target == null) {
                writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "program not found");
                return;
            }
        }

        try {
            // this exists in your code: DisplayAPI#expand(int degree) → ExpandDTO
            ExpandDTO out = target.expand(degree);
            writeJson(resp, HttpServletResponse.SC_OK, out);

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " +
                    (e.getMessage() == null ? "" : e.getMessage());
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, DisplayAPI> getDisplayRegistry(HttpServletRequest req) {
        Object obj = req.getServletContext().getAttribute(ATTR_DISPLAY_REGISTRY);
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, DisplayAPI>) m;
        }
        Map<String, DisplayAPI> created = new ConcurrentHashMap<>();
        req.getServletContext().setAttribute(ATTR_DISPLAY_REGISTRY, created);
        return created;
    }
}
