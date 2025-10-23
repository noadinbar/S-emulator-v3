package application.servlets.authentication;

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import users.UserManager;

import static utils.Constants.*;
import static utils.ServletUtils.writeJson;

@WebServlet(name = "LogoutServlet", urlPatterns = {API_LOGOUT})
public class LogoutServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            HttpSession s = req.getSession(false);
            String uname = s != null ? (String) s.getAttribute(SESSION_USERNAME) : null;
            if (uname != null) {
                UserManager um = (UserManager) getServletContext().getAttribute("userManager");
                if (um != null) um.remove(uname);
                s.invalidate();
            }
            JsonObject ok = new JsonObject();
            ok.addProperty("ok", true);
            writeJson(resp, HttpServletResponse.SC_OK, ok);
        } catch (Exception e) {
            try { writeJson(resp, 500, new JsonObject()); } catch (Exception ignore) {}
        }
    }
}
