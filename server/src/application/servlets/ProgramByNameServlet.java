package application.servlets;

import application.listeners.AppContextListener;
import application.programs.ProgramManager;
import display.DisplayDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

import static utils.Constants.API_PROGRAM_BY_NAME;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "ProgramByNameServlet", urlPatterns = { API_PROGRAM_BY_NAME })
public class ProgramByNameServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "missing program name");
            return;
        }
        ProgramManager pm = (ProgramManager) getServletContext().getAttribute(AppContextListener.ATTR_PROGRAMS);
        DisplayDTO dto = (pm != null) ? pm.get(name) : null;
        if (dto == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "program not found: " + name);
            return;
        }
        writeJson(resp, HttpServletResponse.SC_OK, dto);
    }
}
