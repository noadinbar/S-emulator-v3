package application.servlets;

import api.DisplayAPI;
import display.DisplayDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static utils.Constants.ATTR_DISPLAY_API;
import static utils.Constants.API_FUNCTIONS;   // "/api/functions"
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "FunctionsServlet", urlPatterns = {API_FUNCTIONS, API_FUNCTIONS + "/*"})
public class FunctionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_API);
        if (!(obj instanceof DisplayAPI)) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "No loaded program.");
            return;
        }
        DisplayAPI display = (DisplayAPI) obj;
        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            // 1) list
            handleList(display, resp);
            return;
        }

        String[] parts = path.split("/");
        if (parts.length < 3) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid functions path.");
            return;
        }
        String key    = urlDecode(parts[1]);
        String action = parts[2];

        if ("program".equals(action)) {
            // 2) program-as-is
            handleProgram(display, key, resp);
        } else {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown action: " + action);
        }
    }

    private void handleList(DisplayAPI display, HttpServletResponse resp) throws IOException {
        List<String> keys = new ArrayList<>();
        try {
            Map<String, DisplayAPI> map = display.functionDisplaysByUserString();
            if (map != null) keys.addAll(map.keySet());
        } catch (Exception ignore) { /* נשאיר רשימה ריקה */ }
        writeJson(resp, HttpServletResponse.SC_OK, keys);
    }

    private void handleProgram(DisplayAPI display, String key, HttpServletResponse resp) throws IOException {
        try {
            Map<String, DisplayAPI> map = display.functionDisplaysByUserString();
            if (map == null || !map.containsKey(key)) {
                writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found: " + key);
                return;
            }
            DisplayAPI func = map.get(key);
            DisplayDTO dto = func.getDisplay();
            writeJson(resp, HttpServletResponse.SC_OK, dto);
        } catch (Exception e) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load function: " + e.getMessage());
        }
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
