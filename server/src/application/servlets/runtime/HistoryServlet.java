package application.servlets.runtime;

import application.history.HistoryManager;
import application.history.HistoryTableRow;
import application.listeners.AppContextListener;
import execution.RunHistoryEntryDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.Constants.API_HISTORY;
import static utils.Constants.SESSION_USERNAME;
import static utils.ServletUtils.writeJson;

@WebServlet(name = "HistoryServlet", urlPatterns = { API_HISTORY })
public class HistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String usernameToShow = req.getParameter("user");

        if (usernameToShow == null || usernameToShow.isBlank()) {
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute(SESSION_USERNAME) != null) {
                usernameToShow = session.getAttribute(SESSION_USERNAME).toString();
            }
        }

        List<RunHistoryEntryDTO> dtoList;
        if (usernameToShow == null || usernameToShow.isBlank()) {
            dtoList = Collections.emptyList();
        } else {
            HistoryManager hm = AppContextListener.getHistory(getServletContext());
            List<HistoryTableRow> rows = hm.getUserHistoryRows(usernameToShow);

            // Convert server rows -> DTOs for the client
            List<RunHistoryEntryDTO> tmp = new ArrayList<>(rows.size());
            for (HistoryTableRow row : rows) {
                RunHistoryEntryDTO dto = new RunHistoryEntryDTO(
                        row.getRunNumber(),
                        row.getUsername(),
                        row.getTargetType(),
                        row.getTargetName(),
                        row.getArchitectureType(),
                        row.getDegree(),
                        row.getFinalY(),
                        row.getCyclesCount(),
                        row.getInputs(),
                        row.getOutputsSnapshot(),
                        row.getRunMode()
                );
                tmp.add(dto);
            }
            dtoList = tmp;
        }
        writeJson(resp, HttpServletResponse.SC_OK, dtoList);
    }
}
