package application.servlets.runtime;

import api.DisplayAPI;
import api.LoadAPI;
import application.execution.ExecutionCache;
import application.functions.FunctionManager;
import application.functions.FunctionTableRow;
import application.programs.ProgramTableRow;
import display.DisplayDTO;
import display.UploadResultDTO;
import exportToDTO.LoadAPIImpl;
import application.execution.ProgramLocks;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.util.concurrent.locks.ReadWriteLock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import application.listeners.AppContextListener;
import application.programs.ProgramManager;

import java.nio.file.Paths;
import java.util.Map;

import users.UserManager;

import static utils.Constants.*;
import static utils.ServletUtils.*;

/**
 * Receives an XML via multipart/form-data ("file"), validates it with LoadAPI,
 * registers { programName -> DisplayDTO } in ProgramManager (server-wide),
 * and returns DisplayDTO as JSON.
 */
@WebServlet(name = "LoadServlet", urlPatterns = { API_LOAD })
@MultipartConfig
public class LoadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1) Parse multipart
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

        // 2) Copy to temp (LoadAPI works with Path)
        Path tmp = Files.createTempFile("program-", ".xml");
        try (InputStream in = filePart.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "failed to read uploaded file");
            return;
        }

        try {
            // 3) Validate & build DTO (עבודת parsing/בדיקות — ללא נעילה)
            LoadAPI loader = new LoadAPIImpl();
            DisplayAPI display = loader.loadFromXml(tmp); // throws on invalid/semantic errors
            DisplayDTO dto = display.getDisplay();        // מה שנחזיר וגם נרשום

            // גזירת שם בסיסי מהקובץ
            String submitted = filePart.getSubmittedFileName(); // e.g. "MyProg.xml"
            String baseName = submitted != null
                    ? Paths.get(submitted).getFileName().toString()
                    : "program";
            baseName = baseName.replaceFirst("\\.[^.]+$", ""); // strip extension

            String uploader = "anonymous";
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("username") != null)
                uploader = session.getAttribute("username").toString();

            ProgramManager pm = (ProgramManager) getServletContext().getAttribute(AppContextListener.ATTR_PROGRAMS);
            FunctionManager fm = (FunctionManager) getServletContext().getAttribute(AppContextListener.ATTR_FUNCTIONS);
            UserManager um    = (UserManager) getServletContext().getAttribute(AppContextListener.ATTR_USERS);

            final ReadWriteLock rw = ProgramLocks.lockFor("REPO");
            rw.writeLock().lock();
            try {
                getServletContext().setAttribute(ATTR_DISPLAY_API, display);
                ExecutionCache.clearAll();
                if (pm != null) {
                    pm.put(baseName, dto);
                    int maxDegree = 0;
                    try { maxDegree = display.execution().getMaxDegree(); } catch (Exception ignore) {}

                    pm.putRecord(new ProgramTableRow(
                            baseName,
                            uploader,
                            dto.numberOfInstructions(),
                            maxDegree
                    ));

                    if (um != null && uploader != null && !uploader.isBlank()) {
                        try { um.onMainProgramUploaded(uploader); } catch (Exception ignore) {}
                    }
                }

                // רישום פונקציות שנגזרו מהתוכנית
                if (fm != null) {
                    Map<String, DisplayAPI> fnMap = display.functionDisplaysByUserString();
                    for (Map.Entry<String, DisplayAPI> e : fnMap.entrySet()) {
                        String userString = e.getKey();
                        DisplayAPI fApi   = e.getValue();
                        DisplayDTO fDto   = fApi.getDisplay();
                        int fBaseInstr = fDto.numberOfInstructions();
                        int fMaxDegree = 0;
                        try { fMaxDegree = fApi.execution().getMaxDegree(); } catch (Exception ignore) {}

                        fm.put(userString, fDto);
                        fm.putRecord(new FunctionTableRow(
                                userString,
                                baseName,
                                uploader,
                                fBaseInstr,
                                fMaxDegree
                        ));

                        if (um != null && uploader != null && !uploader.isBlank()) {
                            try { um.onFunctionUploaded(uploader); } catch (Exception ignore) {}
                        }
                    }
                }

            } finally {
                rw.writeLock().unlock();
            }
            // ========== /NEW ==========

            UploadResultDTO uploadResult = new UploadResultDTO(baseName);
            writeJson(resp, HttpServletResponse.SC_CREATED, uploadResult);

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, msg);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignore) {}
        }
    }

}
