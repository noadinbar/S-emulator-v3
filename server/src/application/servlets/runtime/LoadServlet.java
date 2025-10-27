package application.servlets.runtime;

import api.DisplayAPI;
import api.LoadAPI;
import application.functions.FunctionManager;
import application.functions.FunctionTableRow;
import application.listeners.AppContextListener;
import application.programs.ProgramManager;
import application.programs.ProgramTableRow;
import display.DisplayDTO;
import display.FunctionDTO;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;
import display.UploadResultDTO;
import exportToDTO.DisplayAPIImpl;
import exportToDTO.LoadAPIImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import structure.function.Function;
import users.UserManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Constants.API_LOAD;
import static utils.Constants.ATTR_DISPLAY_REGISTRY;
import static utils.Constants.CHARSET_UTF8;
import static utils.Constants.CONTENT_TYPE_JSON;
import static utils.Constants.HEADER_CONTENT_TYPE;
import static utils.Constants.PART_FILE;
import static utils.Constants.SESSION_USERNAME;
import static utils.ServletUtils.GSON;

@WebServlet(name = "LoadServlet", urlPatterns = { API_LOAD })
@MultipartConfig
public class LoadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws IOException {

        // Step 1: parse multipart/form-data and copy uploaded file to temp
        final Part filePart;
        try {
            filePart = req.getPart(PART_FILE);
        } catch (ServletException e) {
            writeUploadResult(resp,
                    HttpServletResponse.SC_OK,
                    UploadResultDTO.error("request is not multipart/form-data"));
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            writeUploadResult(resp,
                    HttpServletResponse.SC_OK,
                    UploadResultDTO.error("missing file part"));
            return;
        }

        Path tmp = Files.createTempFile("program-", ".xml");
        try (InputStream in = filePart.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            writeUploadResult(resp,
                    HttpServletResponse.SC_OK,
                    UploadResultDTO.error("failed to read uploaded file"));
            return;
        }

        try {
            // Step 2: engine parsing and base validation (XML format, labels, etc.)
            LoadAPI loader = new LoadAPIImpl();
            DisplayAPI display = loader.loadFromXml(tmp); // may throw if XML/labels invalid
            DisplayAPIImpl uploadedImpl = (DisplayAPIImpl) display;

            // Snapshot DTO BEFORE linking external functions
            DisplayDTO dtoBeforeLink = display.getDisplay();

            // Derive program base name from uploaded filename (strip extension)
            String submitted = filePart.getSubmittedFileName();
            String baseName = (submitted != null)
                    ? Paths.get(submitted).getFileName().toString()
                    : "program";
            baseName = baseName.replaceFirst("\\.[^.]+$", "");

            // Resolve uploader (for ProgramTableRow / FunctionTableRow / UserManager)
            String uploader = "anonymous";
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute(SESSION_USERNAME) != null) {
                uploader = session.getAttribute(SESSION_USERNAME).toString();
            }

            // Managers and registry from servlet context
            ProgramManager pm = (ProgramManager)
                    getServletContext().getAttribute(AppContextListener.ATTR_PROGRAMS);
            FunctionManager fm = (FunctionManager)
                    getServletContext().getAttribute(AppContextListener.ATTR_FUNCTIONS);
            UserManager um = (UserManager)
                    getServletContext().getAttribute(AppContextListener.ATTR_USERS);
            Map<String, DisplayAPI> registry = getDisplayRegistry();

            // --------------------------
            // Step 3: business validation (no mutation yet)
            // --------------------------

            // 3a. Program name must be unique globally
            if (pm != null && pm.exists(baseName)) {
                writeUploadResult(resp,
                        HttpServletResponse.SC_OK,
                        UploadResultDTO.error(
                                "Program name '" + baseName + "' already exists"));
                return;
            }

            // 3b. Collect local function userStrings from THIS upload,
            //     detect duplicates inside the same file
            Set<String> localUserStrings = new LinkedHashSet<>();
            Set<String> dupInFile = new LinkedHashSet<>();

            List<FunctionDTO> funcDtos = dtoBeforeLink.getFunctions();
            if (funcDtos != null) {
                for (FunctionDTO fDto : funcDtos) {
                    if (fDto == null) {
                        continue;
                    }
                    String us = fDto.getUserString();
                    if (us == null || us.isBlank()) {
                        continue;
                    }
                    if (!localUserStrings.add(us)) {
                        dupInFile.add(us);
                    }
                }
            }

            if (!dupInFile.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Duplicate function(s) in file: ");
                boolean firstDup = true;
                for (String us : dupInFile) {
                    if (!firstDup) {
                        sb.append(", ");
                    }
                    sb.append("'").append(us).append("'");
                    firstDup = false;
                }
                writeUploadResult(resp,
                        HttpServletResponse.SC_OK,
                        UploadResultDTO.error(sb.toString()));
                return;
            }

            // 3c. No shadowing of global functions:
            //     any brand-new function userString in THIS upload
            //     must not already exist in FunctionManager
            if (fm != null) {
                for (String us : localUserStrings) {
                    if (fm.exists(us)) {
                        writeUploadResult(resp,
                                HttpServletResponse.SC_OK,
                                UploadResultDTO.error(
                                        "Function '" + us + "' already exists in the system"));
                        return;
                    }
                }
            }

            // Step 4: dependency resolution with fixpoint
            while (true) {
                // Collect the calls from the current program snapshot
                DisplayDTO currentDto = display.getDisplay();
                Set<String> needed = extractCalledFunctionUserStrings(currentDto);

                // What functions are currently inside the uploaded program
                List<String> haveList = uploadedImpl.listFunctionUserStrings();
                Set<String> haveSet = new LinkedHashSet<>(haveList);

                // Prepare a batch of external Function objects to attach
                List<Function> batchToAttach = new LinkedList<>();

                // Track unresolved calls we cannot satisfy from registry
                Set<String> unresolved = new LinkedHashSet<>();

                for (String callName : needed) {
                    if (callName == null || callName.isBlank()) {
                        continue;
                    }
                    if (haveSet.contains(callName)) {
                        // already provided by current program
                        continue;
                    }

                    // resolve from registry using the function userString as key
                    DisplayAPI depApi = registry.get(callName);
                    if (depApi == null) {
                        unresolved.add(callName);
                        continue;
                    }

                    DisplayAPIImpl depImpl = (DisplayAPIImpl) depApi;
                    Function externalFn = depImpl.findFunctionByUserString(callName);
                    if (externalFn == null) {
                        unresolved.add(callName);
                        continue;
                    }

                    // queue this external function to be attached
                    haveSet.add(callName);
                    batchToAttach.add(externalFn);
                }

                // if we have unresolved calls -> fail now, no commit
                if (!unresolved.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown function(s): ");
                    boolean firstMiss = true;
                    for (String us : unresolved) {
                        if (!firstMiss) {
                            sb.append(", ");
                        }
                        sb.append("'").append(us).append("'");
                        firstMiss = false;
                    }
                    writeUploadResult(resp,
                            HttpServletResponse.SC_OK,
                            UploadResultDTO.error(sb.toString()));
                    return;
                }

                // if there is nothing new to attach -> fixpoint reached
                if (batchToAttach.isEmpty()) {
                    break;
                }

                // otherwise, attach this batch to the uploaded program.
                // This mutates the engine Program internally.
                uploadedImpl.attachFunctions(batchToAttach);

                // then loop again, because attached functions might call more
            }

            // After the loop: program is now "closed" under dependencies.
            // Rebuild final DTO reflecting attached external functions.
            DisplayDTO finalDto = display.getDisplay();

            // --------------------------
            // Step 5: commit to global state
            // --------------------------

            // 5a. registry keys:
            //     - baseName -> full program DisplayAPI
            //     - each function userString -> function-scoped DisplayAPI
            Map<String, DisplayAPI> fnMap = display.functionDisplaysByUserString();

            Map<String, DisplayAPI> globalRegistry = getDisplayRegistry();
            globalRegistry.put(baseName, display);
            for (Map.Entry<String, DisplayAPI> entry : fnMap.entrySet()) {
                String userString = entry.getKey();
                DisplayAPI fApi = entry.getValue();
                globalRegistry.put(userString, fApi);
            }

            // 5b. ProgramManager
            if (pm != null) {
                pm.put(baseName, finalDto);

                int maxDegree = 0;
                try {
                    maxDegree = display.execution().getMaxDegree();
                } catch (Exception ignore) {
                    // execution().getMaxDegree() may throw in some cases
                }

                pm.putRecord(new ProgramTableRow(
                        baseName,
                        uploader,
                        finalDto.numberOfInstructions(),
                        maxDegree
                ));

                if (um != null && uploader != null && !uploader.isBlank()) {
                    try {
                        um.onMainProgramUploaded(uploader);
                    } catch (Exception ignore) {
                        // ignore user stats failure
                    }
                }
            }

            // 5c. FunctionManager
            // Register ONLY the brand-new functions from this upload.
            // We do not re-register imported external functions.
            if (fm != null) {
                for (Map.Entry<String, DisplayAPI> entry : fnMap.entrySet()) {
                    String userString = entry.getKey();

                    // only if userString originally came from this file
                    if (!localUserStrings.contains(userString)) {
                        continue;
                    }

                    DisplayAPI fApi = entry.getValue();
                    DisplayDTO fDto = fApi.getDisplay();

                    int fInstrCount = fDto.numberOfInstructions();
                    int fMaxDegree = 0;
                    try {
                        fMaxDegree = fApi.execution().getMaxDegree();
                    } catch (Exception ignore) {
                        // ignore
                    }

                    fm.put(userString, fDto);
                    fm.putRecord(new FunctionTableRow(
                            userString,
                            baseName,
                            uploader,
                            fInstrCount,
                            fMaxDegree
                    ));

                    if (um != null && uploader != null && !uploader.isBlank()) {
                        try {
                            um.onFunctionUploaded(uploader);
                        } catch (Exception ignore) {
                            // ignore
                        }
                    }
                }
            }

            // --------------------------
            // Step 6: respond success to client
            // --------------------------
            List<String> addedFns = new ArrayList<>(localUserStrings);
            Collections.sort(addedFns);

            UploadResultDTO okBody = new UploadResultDTO(baseName, addedFns);

            writeUploadResult(resp,
                    HttpServletResponse.SC_OK,
                    okBody);

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " +
                    (e.getMessage() == null ? "" : e.getMessage());

            writeUploadResult(resp,
                    HttpServletResponse.SC_OK,
                    UploadResultDTO.error(msg));

        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignore) {
                // best-effort cleanup
            }
        }
    }

    /**
     * Collect all called function userStrings from:
     * - main program instructions
     * - every function body in this DisplayDTO
     */
    private Set<String> extractCalledFunctionUserStrings(DisplayDTO dto) {
        Set<String> out = new LinkedHashSet<>();

        collectCallsFrom(dto.getInstructions(), out);

        List<FunctionDTO> funcs = dto.getFunctions();
        if (funcs != null) {
            for (FunctionDTO f : funcs) {
                if (f == null) {
                    continue;
                }
                collectCallsFrom(f.getInstructions(), out);
            }
        }

        return out;
    }

    /**
     * For each instruction, if the op means "call a function" and carries a userString,
     * record that userString.
     */
    private void collectCallsFrom(List<InstructionDTO> instructions, Set<String> out) {
        if (instructions == null) {
            return;
        }

        for (InstructionDTO ins : instructions) {
            if (ins == null) {
                continue;
            }

            InstructionBodyDTO body = ins.getBody();
            if (body == null) {
                continue;
            }

            InstrOpDTO op = body.getOp();
            if (op == null) {
                continue;
            }

            // Update this list if there are more call-like ops in your InstructionBodyDTO
            if (op == InstrOpDTO.QUOTE || op == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                String us = body.getUserString();
                if (us != null && !us.isBlank()) {
                    out.add(us.trim());
                }
            }
        }
    }

    /**
     * Serialize UploadResultDTO with JSON. Always return 200 from servlet logic.
     */
    private void writeUploadResult(HttpServletResponse resp,
                                   int httpStatus,
                                   UploadResultDTO body) throws IOException {
        resp.setStatus(httpStatus);
        resp.setHeader(HEADER_CONTENT_TYPE,
                CONTENT_TYPE_JSON + "; charset=" + CHARSET_UTF8);
        GSON.toJson(body, resp.getWriter());
    }

    @SuppressWarnings("unchecked")
    private Map<String, DisplayAPI> getDisplayRegistry() {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_REGISTRY);
        if (obj instanceof Map) {
            return (Map<String, DisplayAPI>) obj;
        }

        Map<String, DisplayAPI> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(ATTR_DISPLAY_REGISTRY, created);
        return created;
    }
}
