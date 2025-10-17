package application.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.Map;

import api.DisplayAPI;

import static utils.Constants.*;
@WebServlet(name = "StatusServlet", urlPatterns = {API_STATUS})
public class StatusServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        JsonObject json = new JsonObject();
        DisplayAPI display = (DisplayAPI) getServletContext().getAttribute(ATTR_DISPLAY_API);

        boolean loaded = (display != null && display.getDisplay() != null);
        json.addProperty("loaded", loaded);

        if (loaded) {
            String programName = display.getDisplay().getProgramName();
            int maxDegree = 0;
            try {
                maxDegree = display.execution().getMaxDegree();
            } catch (Exception ignore) {  }

            json.addProperty("programName", programName);
            json.addProperty("maxDegree", maxDegree);

            JsonObject functionsMax = new JsonObject();
            try {
                Map<String, DisplayAPI> funcs = display.functionDisplaysByUserString();
                if (funcs != null) {
                    for (Map.Entry<String, DisplayAPI> e : funcs.entrySet()) {
                        int fMax = 0;
                        try {
                            fMax = e.getValue().execution().getMaxDegree();
                        } catch (Exception ignore) { /* נשאיר 0 */ }
                        functionsMax.addProperty(e.getKey(), fMax);
                    }
                }
            } catch (Exception ignore) {  }
            json.add("functionsMaxDegrees", functionsMax);

        } else {
            json.addProperty("programName", (String) null);
            json.addProperty("maxDegree", 0);
            json.add("functionsMaxDegrees", new JsonObject()); // אובייקט ריק כשלא נטען
        }

        Boolean execBusy = (Boolean) getServletContext().getAttribute("execBusy");
        Boolean dbgBusy  = (Boolean) getServletContext().getAttribute("dbgBusy");
        boolean eb = execBusy != null && execBusy;
        boolean db = dbgBusy  != null && dbgBusy;

        json.addProperty("executeBusy", eb);
        json.addProperty("debugBusy",   db);
        json.addProperty("mode", db ? "DEBUG" : (eb ? "EXECUTE" : MODE_IDLE));

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(gson.toJson(json));
    }
}
