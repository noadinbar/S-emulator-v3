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
import java.util.*;
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
            Map<String, String> nameIndex = getFunctionNameMap();

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

            // Step 4: dependency resolution with fixpoint (fixed)

            while (true) {

                // Snapshot after any functions we've already attached
                DisplayDTO currentDto = display.getDisplay();

                // Map of local functions in the current program right now:
                // functionName (internal) -> userString (alias)
                Map<String, String> inProgramNameToUserString = new HashMap<String, String>();

                // All userStrings that are already part of this program
                // (functions that are currently attached)
                List<String> haveList = uploadedImpl.listFunctionUserStrings();
                Set<String> haveSet = new LinkedHashSet<String>(haveList);

                List<FunctionDTO> funcsNow = currentDto.getFunctions();
                if (funcsNow != null) {
                    for (FunctionDTO f : funcsNow) {
                        if (f == null) {
                            continue;
                        }
                        String fName = f.getName();
                        String fUser = f.getUserString();
                        if (fName != null && !fName.isBlank()
                                && fUser != null && !fUser.isBlank()) {
                            inProgramNameToUserString.put(fName, fUser);
                            haveSet.add(fUser);
                        }
                    }
                }

                // Collect all function calls (direct QUOTE/JUMP_EQUAL_FUNCTION
                // and nested/composed calls in their arguments).
                // namesCalled can contain either internal function names
                // (e.g. "Smaller_Equal_Than") OR already the userString alias
                // (e.g. "<=") depending on how the DTO was built.
                // ustrCalled are explicit userStrings on the instruction body.
                Set<String> namesCalled = new LinkedHashSet<String>();
                Set<String> ustrCalled  = new LinkedHashSet<String>();
                collectCalledFunctionsFromDisplayDTO(
                        currentDto,
                        namesCalled,
                        ustrCalled
                );

                // Turn all collected identifiers into "aliases we need".
                // An alias here means "the userString of a function we depend on".
                //
                // For each identifier 'id' from namesCalled:
                // 1. If it's a known local functionName -> take its userString.
                // 2. Else if it's already present as a local userString
                //    (haveSet.contains(id)) -> treat it as that alias.
                // 3. Else if global nameIndex knows id as functionName -> map to that alias.
                // 4. Else fall back to treating 'id' itself as the alias. This covers
                //    the case where 'id' is actually a userString defined in some
                //    program we already loaded into the registry.

                Set<String> neededAliases = new LinkedHashSet<>();

                for (String id : namesCalled) {
                    if (id == null || id.isBlank()) {
                        continue;
                    }

                    String alias = resolveAliasForName(
                            id,
                            inProgramNameToUserString,
                            haveSet,
                            nameIndex,
                            registry
                    );

                    if (alias != null && !alias.isBlank()) {
                        neededAliases.add(alias.trim());
                    }
                }


                // Also include direct userStrings taken from instruction bodies
                for (String u : ustrCalled) {
                    if (u == null || u.isBlank()) {
                        continue;
                    }
                    neededAliases.add(u.trim());
                }

                // Now figure out which aliases we still do NOT have locally
                neededAliases.removeAll(haveSet);
                if (neededAliases.isEmpty()) {
                    // We have all required functions already
                    break;
                }

                // Try to import those missing aliases from the global registry.
                // registry is userString -> DisplayAPI
                List<Function> batchToAttach = new LinkedList<Function>();
                Set<String> unresolvedAlias = new LinkedHashSet<String>();

                for (String alias : neededAliases) {
                    if (alias == null || alias.isBlank()) {
                        continue;
                    }

                    DisplayAPI depApi = registry.get(alias);
                    if (depApi == null) {
                        // We do not have an implementation for this alias anywhere
                        unresolvedAlias.add(alias);
                        continue;
                    }

                    DisplayAPIImpl depImpl = (DisplayAPIImpl) depApi;
                    Function externalFn = depImpl.findFunctionByUserString(alias);
                    if (externalFn == null) {
                        unresolvedAlias.add(alias);
                        continue;
                    }

                    // Queue this external function to be attached into the uploaded program
                    haveSet.add(alias);
                    batchToAttach.add(externalFn);
                }

                // If some aliases are still unresolved after checking registry,
                // the upload is invalid: those functions truly do not exist anywhere.
                if (!unresolvedAlias.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown function(s): ");
                    boolean firstMiss = true;
                    for (String us : unresolvedAlias) {
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

                // If there is nothing new to attach, we are done
                if (batchToAttach.isEmpty()) {
                    break;
                }

                // Otherwise attach the external functions and loop again.
                uploadedImpl.attachFunctions(batchToAttach);
            }

            // After fixpoint we rebuild the final DTO
            DisplayDTO finalDto = display.getDisplay();
            DisplayDTO cleanDto = buildCleanDisplayDTO(finalDto);

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
                // store the cleaned DTO (no null userString in QUOTE rows)
                pm.put(baseName, cleanDto);

                int maxDegree = 0;
                try {
                    maxDegree = display.execution().getMaxDegree();
                } catch (Exception ignore) {
                    // ignore
                }

                pm.putRecord(new ProgramTableRow(
                        baseName,
                        uploader,
                        cleanDto.numberOfInstructions(),
                        maxDegree
                ));

                if (um != null && uploader != null && !uploader.isBlank()) {
                    try {
                        um.onMainProgramUploaded(uploader);
                    } catch (Exception ignore) {
                        // ignore
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

            // 5d. Update the global functionName -> userString index.
            if (nameIndex != null) {
                List<FunctionDTO> finalFunctions = cleanDto.getFunctions();
                if (finalFunctions != null) {
                    for (FunctionDTO f : finalFunctions) {
                        if (f == null) {
                            continue;
                        }
                        String fname = f.getName();
                        String ustr  = f.getUserString();
                        if (fname != null && !fname.isBlank()
                                && ustr != null && !ustr.isBlank()) {
                            nameIndex.put(fname, ustr);
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

    private void collectFunctionsFromArgs(String argsText, Set<String> namesOut) {
        if (argsText == null) {
            return;
        }
        String text = argsText.trim();
        if (text.isEmpty()) {
            return;
        }
        // Split into top-level arguments, same logic as buildArgToXMap / expand(...)
        List<String> parts = splitTopArguments(text);

        for (String raw : parts) {
            if (raw == null) {
                continue;
            }
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }

            if (token.startsWith("(") && token.endsWith(")")) {
                String[] fa = splitFuncNameAndArgs(token);
                String fname = fa[0];
                String innerArgs = fa[1];
                if (fname != null && !fname.isBlank()) {
                    namesOut.add(fname.trim());
                }
                collectFunctionsFromArgs(innerArgs, namesOut);
                continue;
            }
        }
    }

    private void collectCalledFunctionsFromInstructionList(
            List<InstructionDTO> instructions,
            Set<String> namesOut,
            Set<String> userStringsOut) {

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

            if (op == InstrOpDTO.QUOTE || op == InstrOpDTO.JUMP_EQUAL_FUNCTION) {

                // Direct function reference by internal name
                String fName = body.getFunctionName();
                if (fName != null && !fName.isBlank()) {
                    namesOut.add(fName.trim());
                }

                // Direct userString reference (may already be known if defined in this file)
                String ustr = body.getUserString();
                if (ustr != null && !ustr.isBlank()) {
                    userStringsOut.add(ustr.trim());
                }

                // Now parse nested/composed calls from the arguments string
                String argsText = body.getFunctionArgs();
                collectFunctionsFromArgs(argsText, namesOut);
            }
        }
    }

    private void collectCalledFunctionsFromDisplayDTO(
            DisplayDTO dto,
            Set<String> namesOut,
            Set<String> userStringsOut) {

        if (dto == null) {
            return;
        }

        // main instructions of the program
        collectCalledFunctionsFromInstructionList(
                dto.getInstructions(),
                namesOut,
                userStringsOut
        );

        List<FunctionDTO> funcs = dto.getFunctions();
        if (funcs != null) {
            for (FunctionDTO f : funcs) {
                if (f == null) {
                    continue;
                }
                collectCalledFunctionsFromInstructionList(
                        f.getInstructions(),
                        namesOut,
                        userStringsOut
                );
            }
        }
    }

    private String resolveAliasForName(String internalName,
                                       Map<String, String> inProgramNameToUserString,
                                       Set<String> haveSet,
                                       Map<String, String> nameIndex,
                                       Map<String, DisplayAPI> registry) {

        if (internalName == null || internalName.isBlank()) {
            return "";
        }

        // 1. local mapping inside the current uploaded program
        String localAlias = inProgramNameToUserString.get(internalName);
        if (localAlias != null && !localAlias.isBlank()) {
            return localAlias.trim();
        }

        // 2. maybe the internalName is itself already a userString we attached
        if (haveSet.contains(internalName)) {
            return internalName.trim();
        }

        // 3. global map from previous uploads: functionName -> userString
        String globalAlias = nameIndex.get(internalName);
        if (globalAlias != null && !globalAlias.isBlank()) {
            return globalAlias.trim();
        }

        // 4. last resort: scan all known functions in the registry
        //    and see if any of them declares this internalName.
        for (DisplayAPI api : registry.values()) {
            if (api == null) {
                continue;
            }
            DisplayDTO d = api.getDisplay();
            List<FunctionDTO> fList = d.getFunctions();
            if (fList == null) {
                continue;
            }
            for (FunctionDTO f : fList) {
                if (f == null) {
                    continue;
                }
                String fName = f.getName();
                String fUser = f.getUserString();
                if (fName == null || fUser == null) {
                    continue;
                }
                if (fName.isBlank() || fUser.isBlank()) {
                    continue;
                }
                if (fName.equals(internalName)) {
                    // learn this mapping for future uploads in this JVM
                    nameIndex.put(fName, fUser);
                    return fUser.trim();
                }
            }
        }

        // 5. could not resolve. return the internal name as-is.
        return internalName.trim();
    }

    // Build a cleaned clone of the DisplayDTO so UI will never see null for function calls.
    // We only touch QUOTE and JUMP_EQUAL_FUNCTION instructions.
    private DisplayDTO buildCleanDisplayDTO(DisplayDTO dto) {

        if (dto == null) {
            return null;
        }

        // aliasMap: internal function name -> userString from the final program
        // example: "Minus" -> "-", "Smaller_Than" -> "<", "AND" -> "&&"
        Map<String, String> aliasMap = new HashMap<>();
        List<FunctionDTO> origFuncs = dto.getFunctions();
        if (origFuncs != null) {
            for (FunctionDTO f : origFuncs) {
                if (f == null) {
                    continue;
                }
                String fname = f.getName();
                String ustr  = f.getUserString();
                if (fname != null && !fname.isBlank()
                        && ustr != null && !ustr.isBlank()) {
                    aliasMap.put(fname, ustr);
                }
            }
        }

        // Clone main program instructions with fixed userString where needed
        List<InstructionDTO> fixedMain = new ArrayList<>();
        List<InstructionDTO> origMain = dto.getInstructions();
        if (origMain != null) {
            for (InstructionDTO ins : origMain) {

                if (ins == null) {
                    fixedMain.add(null);
                    continue;
                }

                InstructionBodyDTO body = ins.getBody();
                InstrOpDTO op = (body == null ? null : body.getOp());

                // Only rewrite QUOTE / JUMP_EQUAL_FUNCTION
                if (op != InstrOpDTO.QUOTE && op != InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    fixedMain.add(ins);
                    continue;
                }

                String funcName   = body.getFunctionName();   // internal name, e.g. "Minus"
                String aliasShown = body.getUserString();     // may be null
                String args       = body.getFunctionArgs();   // "(Minus,x1,x2)" etc.

                // Choose alias to show:
                // 1. if body already had userString -> keep it
                // 2. else use aliasMap.get(funcName)
                // 3. else fall back to funcName itself
                if (aliasShown == null || aliasShown.isBlank()) {
                    String mapped = (funcName == null ? null : aliasMap.get(funcName));
                    if (mapped != null && !mapped.isBlank()) {
                        aliasShown = mapped;
                    } else if (funcName != null && !funcName.isBlank()) {
                        aliasShown = funcName;
                    } else {
                        aliasShown = "";
                    }
                }

                InstructionBodyDTO newBody;
                if (op == InstrOpDTO.QUOTE) {
                    newBody = new InstructionBodyDTO(
                            op,
                            body.getVariable(),   // result var (destination)
                            null,
                            null,
                            null,
                            null,
                            0L,
                            null,
                            funcName,             // internal name
                            aliasShown,           // fixed alias for UI
                            args
                    );
                } else {
                    // JUMP_EQUAL_FUNCTION
                    newBody = new InstructionBodyDTO(
                            op,
                            body.getVariable(),   // variable to test
                            null,
                            null,
                            body.getCompare(),    // compare var
                            null,
                            0L,
                            body.getJumpTo(),     // jump target label
                            funcName,
                            aliasShown,
                            args
                    );
                }

                InstructionDTO newDTO = new InstructionDTO(
                        ins.getNumber(),
                        ins.getKind(),
                        ins.getLabel(),
                        newBody,
                        ins.getCycles(),
                        ins.getGeneration()
                );

                fixedMain.add(newDTO);
            }
        }

        // Clone each function with same logic so inside functions also looks nice
        List<FunctionDTO> fixedFuncs = new ArrayList<>();
        if (origFuncs != null) {
            for (FunctionDTO origF : origFuncs) {

                if (origF == null) {
                    fixedFuncs.add(null);
                    continue;
                }

                List<InstructionDTO> fixedBodyList = new ArrayList<>();
                List<InstructionDTO> origBodyList = origF.getInstructions();

                if (origBodyList != null) {
                    for (InstructionDTO ins : origBodyList) {

                        if (ins == null) {
                            fixedBodyList.add(null);
                            continue;
                        }

                        InstructionBodyDTO body = ins.getBody();
                        InstrOpDTO op = (body == null ? null : body.getOp());

                        if (op != InstrOpDTO.QUOTE && op != InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                            fixedBodyList.add(ins);
                            continue;
                        }

                        String funcName   = body.getFunctionName();
                        String aliasShown = body.getUserString();
                        String args       = body.getFunctionArgs();

                        if (aliasShown == null || aliasShown.isBlank()) {
                            String mapped = (funcName == null ? null : aliasMap.get(funcName));
                            if (mapped != null && !mapped.isBlank()) {
                                aliasShown = mapped;
                            } else if (funcName != null && !funcName.isBlank()) {
                                aliasShown = funcName;
                            } else {
                                aliasShown = "";
                            }
                        }

                        InstructionBodyDTO newBody;
                        if (op == InstrOpDTO.QUOTE) {
                            newBody = new InstructionBodyDTO(
                                    op,
                                    body.getVariable(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    0L,
                                    null,
                                    funcName,
                                    aliasShown,
                                    args
                            );
                        } else {
                            newBody = new InstructionBodyDTO(
                                    op,
                                    body.getVariable(),
                                    null,
                                    null,
                                    body.getCompare(),
                                    null,
                                    0L,
                                    body.getJumpTo(),
                                    funcName,
                                    aliasShown,
                                    args
                            );
                        }

                        InstructionDTO newDTO = new InstructionDTO(
                                ins.getNumber(),
                                ins.getKind(),
                                ins.getLabel(),
                                newBody,
                                ins.getCycles(),
                                ins.getGeneration()
                        );
                        fixedBodyList.add(newDTO);
                    }
                }
                fixedFuncs.add(new FunctionDTO(
                        origF.getName(),
                        origF.getUserString(),
                        fixedBodyList
                ));
            }
        }

        // We reuse the rest of the metadata (programName, inputsInUse, labelsInUse)
        return new DisplayDTO( dto.getProgramName(), dto.getInputsInUse(), dto.getLabelsInUse(), fixedMain, fixedFuncs);
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
    private Map<String, String> getFunctionNameMap() {
        Object obj = getServletContext()
                .getAttribute(AppContextListener.ATTR_FUNCTION_NAMES);

        if (obj instanceof Map) {
            return (Map<String, String>) obj;
        }

        // If for some reason it does not exist yet, create it now.
        Map<String, String> created = new ConcurrentHashMap<>();
        getServletContext().setAttribute(AppContextListener.ATTR_FUNCTION_NAMES, created);
        return created;
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

    public List<String> splitTopArguments(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') { depth++; cur.append(c); continue; }
            if (c == ')') { depth--; cur.append(c); continue; }
            if (c == ',' && depth == 0) {
                out.add(cur.toString().trim());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }

    public String[] splitFuncNameAndArgs(String expr) {
        String inner = removeOuterParentheses(expr);
        if (inner == null) return new String[] {"", ""};
        int cut = indexOfTopLevelComma(inner);
        String fname = (cut == -1) ? inner.trim() : inner.substring(0, cut).trim();
        String fargs = (cut == -1) ? "" : inner.substring(cut + 1).trim();
        return new String[] { fname, fargs };
    }

    public String removeOuterParentheses(String s) {
        if (s != null && s.length() >= 2 && s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')') {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    public int indexOfTopLevelComma(String s) {
        if (s == null) return -1;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }
}
