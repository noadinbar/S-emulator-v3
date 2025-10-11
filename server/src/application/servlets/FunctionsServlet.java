package application.servlets;

import api.DisplayAPI;
import display.DisplayDTO;
import display.FunctionDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

        String path = req.getPathInfo(); // null, "/", or "/{key}/program"
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
        DisplayDTO root = getRootDisplay(display);
        List<String> keys = new ArrayList<>();
        if (root != null && root.getFunctions() != null) {
            for (FunctionDTO f : root.getFunctions()) {
                String k = (f.getUserString() != null && !f.getUserString().isBlank())
                        ? f.getUserString() : f.getName();
                if (k != null && !k.isBlank()) keys.add(k);
            }
        }
        writeJson(resp, HttpServletResponse.SC_OK, keys);
    }

    private void handleProgram(DisplayAPI display, String key, HttpServletResponse resp) throws IOException {
        DisplayDTO root = getRootDisplay(display);
        if (root == null || root.getFunctions() == null) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "No functions.");
            return;
        }
        for (FunctionDTO f : root.getFunctions()) {
            String k = (f.getUserString() != null && !f.getUserString().isBlank())
                    ? f.getUserString() : f.getName();
            if (key.equals(k)) {
                DisplayDTO dto = new DisplayDTO(
                        f.getName(),
                        root.getInputsInUse(),
                        root.getLabelsInUse(),
                        f.getInstructions(),
                        root.getFunctions()
                );
                writeJson(resp, HttpServletResponse.SC_OK, dto);
                return;
            }
        }
        writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found: " + key);
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static DisplayDTO getRootDisplay(DisplayAPI display) {
        try {
            return display.getDisplay();
        } catch (Throwable ignore) {
            return null;
        }
    }
}
