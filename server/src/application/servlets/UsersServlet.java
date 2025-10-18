package application.servlets;

import application.listeners.AppContextListener;
import users.UserTableRowDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import users.UserManager;
import users.UserTableRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static utils.Constants.API_USERS;
import static utils.ServletUtils.writeJson;

@WebServlet(name = "UsersServlet", urlPatterns = {API_USERS})
public class UsersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            UserManager um = AppContextListener.getUsers(getServletContext());
            List<UserTableRowDTO> dtos = new ArrayList<>();
            for (UserTableRow row : um.values()) {
                dtos.add(new UserTableRowDTO(
                        row.getName(),
                        row.getMainPrograms(),
                        row.getFunctions(),
                        row.getCreditsCurrent(),
                        row.getCreditsUsed(),
                        row.getRuns()
                ));
            }
            dtos.sort(Comparator.comparing(UserTableRowDTO::getName, String.CASE_INSENSITIVE_ORDER));
            writeJson(resp, HttpServletResponse.SC_OK, dtos);
        } catch (Exception e) {
            try { resp.setStatus(500); resp.getWriter().write("[]"); } catch (Exception ignore) {}
        }
    }
}
