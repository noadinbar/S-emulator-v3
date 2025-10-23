package application.servlets.authentication;

import application.listeners.AppContextListener;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import users.UserManager;
import users.UserTableRow;

import java.io.BufferedReader;
import java.io.IOException;

import static utils.Constants.API_CREDITS_CHARGE;
import static utils.Constants.SESSION_USERNAME;
import static utils.ServletUtils.GSON;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

/**
 * POST /api/credits/charge
 * Top-up credits for the currently logged-in user (positive amount only).
 * Read/modify user state; does NOT touch the engine/DTO layer.
 */
@WebServlet(name = "CreditsServlet", urlPatterns = { API_CREDITS_CHARGE })
public class CreditsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1) Must be logged in (do NOT create a new session here)
        HttpSession session = req.getSession(false);
        if (session == null) {
            writeJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            return;
        }
        String username = (String) session.getAttribute(SESSION_USERNAME);
        if (username == null || username.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Login required");
            return;
        }

        // 2) Parse "amount" (JSON body {"amount": <int>} or fallback to ?amount=)
        Integer amount = readAmount(req);
        if (amount == null || amount <= 0) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Amount must be a positive integer");
            return;

            //TODO: should be error popup
        }

        // 3) Update credits atomically per user
        UserManager um = AppContextListener.getUsers(getServletContext());
        if (um == null) {
            writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Users registry not available");
            return;
        }
        um.adjustCredits(username, amount); // positive = top-up

        // 4) Snapshot after update
        UserTableRow row = um.get(username);
        int current = (row != null) ? row.getCreditsCurrent() : 0;
        int used    = (row != null) ? row.getCreditsUsed()    : 0;

        // 5) Reply a small JSON payload
        JsonObject ok = new JsonObject();
        ok.addProperty("ok", true);
        ok.addProperty("username", username);
        ok.addProperty("creditsCurrent", current);
        ok.addProperty("creditsUsed", used);
        writeJson(resp, HttpServletResponse.SC_OK, ok);
    }

    /** Reads {"amount": <int>} from JSON body; if empty/invalid returns null. */
    private Integer readAmount(HttpServletRequest req) {
        try (BufferedReader br = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String json = sb.toString();
            if (json == null || json.isBlank()) return tryQueryParam(req);
            JsonObject js = GSON.fromJson(json, JsonObject.class);
            if (js != null && js.has("amount") && js.get("amount").isJsonPrimitive()) {
                return js.get("amount").getAsInt();
            }
            return tryQueryParam(req);
        } catch (Exception ignore) {
            return tryQueryParam(req);
        }
    }

    /** Fallback parsing from query param ?amount=... */
    private Integer tryQueryParam(HttpServletRequest req) {
        String qp = req.getParameter("amount");
        if (qp == null || qp.isBlank()) return null;
        try {
            return Integer.parseInt(qp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
