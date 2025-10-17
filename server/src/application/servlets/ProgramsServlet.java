package application.servlets;

import application.listeners.AppContextListener;
import application.programs.ProgramManager;
import application.programs.ProgramTableRow;
import display.ProgramRowDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static utils.Constants.API_PROGRAMS;
import static utils.ServletUtils.writeJson;

/** Returns the full programs table as shared DTOs (all-information-always). */
@WebServlet(name = "ProgramsServlet", urlPatterns = { API_PROGRAMS })
public class ProgramsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ProgramManager pm = (ProgramManager) getServletContext().getAttribute(AppContextListener.ATTR_PROGRAMS);
        if (pm == null) {
            writeJson(resp, HttpServletResponse.SC_OK, List.of());
            return;
        }

        List<ProgramRowDTO> out = pm.listRecords()
                .stream()
                .sorted(Comparator.comparing(ProgramTableRow::getName))
                .map(r -> new ProgramRowDTO(
                        r.getName(),
                        r.getUploader(),
                        r.getBaseInstrCount(),
                        r.getMaxDegree(),
                        r.getRunCount(),
                        r.getAvgCredits()
                ))
                .toList();

        writeJson(resp, HttpServletResponse.SC_OK, out);
    }
}
