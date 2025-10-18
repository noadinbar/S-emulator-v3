// server/src/application/servlets/LoginServlet.java
package application.servlets;

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import users.UserManager;

import static utils.Constants.*;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "LoginServlet", urlPatterns = {API_LOGIN})
public class LoginServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String requested = req.getParameter("username");
            if (requested == null || requested.trim().isEmpty()) {
                writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "username is required");
                return;
            }
            String uname = requested.trim();
            UserManager um = (UserManager) getServletContext().getAttribute("userManager");
            if (um == null) {
                writeJsonError(resp, 500, "user manager not initialized");
                return;
            }

            if (um.exists(uname)) {
                writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "User name already in use");
                return;
            }

            um.addIfAbsent(uname);
            req.getSession(true).setAttribute(SESSION_USERNAME, uname);

            JsonObject ok = new JsonObject();
            ok.addProperty("ok", true);
            ok.addProperty("username", uname);
            writeJson(resp, HttpServletResponse.SC_OK, ok);
        } catch (Exception e) {
            try { writeJsonError(resp, 500, e.getMessage()); } catch (Exception ignore) {}
        }
    }
}
