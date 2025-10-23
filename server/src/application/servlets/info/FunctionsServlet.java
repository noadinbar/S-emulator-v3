package application.servlets.info;

import application.functions.FunctionManager;
import application.functions.FunctionTableRow;
import application.listeners.AppContextListener;
import display.DisplayDTO;
import display.FunctionRowDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static utils.Constants.API_FUNCTIONS;   // "/api/functions"
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

@WebServlet(name = "FunctionsServlet", urlPatterns = {API_FUNCTIONS, API_FUNCTIONS + "/*"})
public class FunctionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FunctionManager fm = (FunctionManager) getServletContext()
                .getAttribute(AppContextListener.ATTR_FUNCTIONS);
        if (fm == null) {
            String path = req.getPathInfo();
            if (path == null || "/".equals(path)) {
                writeJson(resp, HttpServletResponse.SC_OK, List.of());
            } else {
                writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "Functions registry not initialized.");
            }
            return;
        }

        String path = req.getPathInfo();
        if (path == null || "/".equals(path)) {
            handleRows(fm, resp);
            return;
        }

        if ("/keys".equals(path)) {
            writeJson(resp, HttpServletResponse.SC_OK, fm.list());
            return;
        }

        if (path.endsWith("/program")) {
            String rest = path.startsWith("/") ? path.substring(1) : path; // "{key}/program"
            String keyEnc = rest.substring(0, rest.length() - "/program".length()); // strip "/program"
            String key = urlDecode(keyEnc);
            DisplayDTO dto = fm.get(key);
            if (dto == null) {
                writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Function not found: " + key);
                return;
            }
            writeJson(resp, HttpServletResponse.SC_OK, dto);
            return;
        }
        writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown functions endpoint.");
    }

    private void handleRows(FunctionManager fm, HttpServletResponse resp) throws IOException {
        List<FunctionRowDTO> out = new ArrayList<>();
        for (FunctionTableRow r : fm.listRecords()) {
            out.add(new FunctionRowDTO(
                    r.getName(),
                    r.getProgramName(),
                    r.getUploader(),
                    r.getBaseInstrCount(),
                    r.getMaxDegree()
            ));
        }
        writeJson(resp, HttpServletResponse.SC_OK, out);
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
