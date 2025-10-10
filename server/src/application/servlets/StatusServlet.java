package application.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

import api.DisplayAPI;

import static utils.Constants.*; // כדי להשתמש ב-ATTR_DISPLAY_API, MODE וכו'

@WebServlet(name = "StatusServlet", urlPatterns = {"/api/status"})
public class StatusServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        JsonObject json = new JsonObject();

        // ★ לקרוא עם אותו מפתח ששמרנו בו ב-LoadServlet
        DisplayAPI display = (DisplayAPI) getServletContext().getAttribute(ATTR_DISPLAY_API);

        boolean loaded = (display != null && display.getCommand2() != null);
        json.addProperty("loaded", loaded);

        if (loaded) {
            String programName = display.getCommand2().getProgramName();
            int maxDegree = 0;
            try {
                maxDegree = display.execution().getMaxDegree();
            } catch (Exception ignore) { /* נשאיר 0 אם לא זמין */ }

            json.addProperty("programName", programName);
            json.addProperty("maxDegree", maxDegree);
        } else {
            json.addProperty("programName", (String) null);
            json.addProperty("maxDegree", 0);
        }

        // אותם שמות כמו ב-LoadServlet
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
