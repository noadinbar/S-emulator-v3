package application.servlets.info;

import application.listeners.AppContextListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import api.DisplayAPI;
import users.UserManager;
import users.UserTableRow;

import static utils.Constants.*;

@WebServlet(name = "StatusServlet", urlPatterns = {API_STATUS})
public class StatusServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        JsonObject json = new JsonObject();

        // which program are we asking status about?
        String programKey = req.getParameter("program");

        // pull the correct DisplayAPI for that program from the registry
        Map<String, DisplayAPI> registry = getDisplayRegistry();
        DisplayAPI progApi = null;
        if (programKey != null && !programKey.isBlank()) {
            progApi = registry.get(programKey);
        }

        boolean loaded = (progApi != null && progApi.getDisplay() != null);
        json.addProperty("loaded", loaded);

        if (loaded) {
            String progName = progApi.getDisplay().getProgramName();

            int maxDegree = 0;
            try {
                maxDegree = progApi.execution().getMaxDegree();
            } catch (Exception ignore) { }

            json.addProperty("programName", progName);
            json.addProperty("maxDegree", maxDegree);

            // for each function: what's the maxDegree it supports
            JsonObject functionsMax = new JsonObject();
            try {
                Map<String, DisplayAPI> funcs = progApi.functionDisplaysByUserString();
                if (funcs != null) {
                    for (Map.Entry<String, DisplayAPI> e : funcs.entrySet()) {
                        int fMax = 0;
                        try {
                            fMax = e.getValue().execution().getMaxDegree();
                        } catch (Exception ignore) { }
                        functionsMax.addProperty(e.getKey(), fMax);
                    }
                }
            } catch (Exception ignore) { }
            json.add("functionsMaxDegrees", functionsMax);

        } else {
            // programKey not found in registry or no display
            json.addProperty("programName", (String) null);
            json.addProperty("maxDegree", 0);
            json.add("functionsMaxDegrees", new JsonObject());
        }

        // debugBusy/executeBusy snapshot for UI "mode" indicator
        Boolean execBusy = (Boolean) getServletContext().getAttribute("execBusy");
        Boolean dbgBusy  = (Boolean) getServletContext().getAttribute(ATTR_DBG_BUSY);
        boolean eb = execBusy != null && execBusy;
        boolean db = dbgBusy  != null && dbgBusy;

        json.addProperty("executeBusy", eb);
        json.addProperty("debugBusy",   db);
        json.addProperty("mode", db ? "DEBUG" : (eb ? "EXECUTE" : MODE_IDLE));

        // --- User & credits snapshot for the current session ---
        String username = (String) req.getSession(true).getAttribute(SESSION_USERNAME);
        json.addProperty("username", username);

        int creditsCurrent = 0;
        int creditsUsed    = 0;

        if (username != null) {
            UserManager um = AppContextListener.getUsers(getServletContext());
            if (um != null) {
                UserTableRow row = um.get(username);
                if (row != null) {
                    creditsCurrent = row.getCreditsCurrent();
                    creditsUsed    = row.getCreditsUsed();
                }
            }
        }
        json.addProperty("creditsCurrent", creditsCurrent);
        json.addProperty("creditsUsed",    creditsUsed);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(json));
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
}
