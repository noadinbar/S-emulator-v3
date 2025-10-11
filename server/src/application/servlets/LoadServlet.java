package application.servlets;

import api.DisplayAPI;
import api.LoadAPI;
import display.DisplayDTO;
import exportToDTO.LoadAPIImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static utils.Constants.*;
import static utils.ServletUtils.*;

/**
 * Receives an XML file via multipart/form-data (field name: "file"),
 * loads it into the engine using LoadAPI, stores DisplayAPI in context,
 * and returns DisplayDTO as JSON (DTO -> JSON).
 */
@WebServlet(name = "LoadServlet", urlPatterns = { API_LOAD })
@MultipartConfig
public class LoadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1) Parse multipart and validate
        final Part filePart;
        try {
            filePart = req.getPart(PART_FILE); // "file"
        } catch (ServletException e) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "request is not multipart/form-data");
            return;
        }
        if (filePart == null || filePart.getSize() == 0) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "missing file part");
            return;
        }

        // 2) Copy upload to a temp file (LoadAPI works with Path)
        Path tmp = Files.createTempFile("program-", ".xml");
        try (InputStream in = filePart.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "failed to read uploaded file");
            return;
        }

        try {
            // 3) Load engine from XML
            LoadAPI loader = new LoadAPIImpl();
            DisplayAPI display = loader.loadFromXml(tmp);

            // 4) Store shared state in context
            getServletContext().setAttribute(ATTR_DISPLAY_API, display);
            if (getServletContext().getAttribute(ATTR_MODE) == null) {
                getServletContext().setAttribute(ATTR_MODE, MODE_IDLE);
            }
            getServletContext().setAttribute("execBusy", Boolean.FALSE);
            getServletContext().setAttribute("dbgBusy",  Boolean.FALSE);

            // 5) Return DisplayDTO as JSON (DTO -> JSON)
            DisplayDTO dto = display.getDisplay(); // "as-is" program
            writeJson(resp, HttpServletResponse.SC_CREATED, dto);

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, msg);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        }
    }
}
