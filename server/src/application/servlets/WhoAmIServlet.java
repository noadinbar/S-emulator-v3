package application.servlets;

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import static utils.Constants.*;
import static utils.ServletUtils.writeJson;

@WebServlet(name = "WhoAmIServlet", urlPatterns = {API_WHOAMI})
public class WhoAmIServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String uname = (String) req.getSession(true).getAttribute(SESSION_USERNAME);
            JsonObject o = new JsonObject();
            o.addProperty("loggedIn", uname != null);
            o.addProperty("username", uname);
            writeJson(resp, HttpServletResponse.SC_OK, o);
        } catch (Exception e) {
            try { writeJson(resp, 500, new JsonObject()); } catch (Exception ignore) {}
        }
    }
}