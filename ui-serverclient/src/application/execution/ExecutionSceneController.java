package application.execution;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import application.opening.OpeningSceneController;
import client.requests.runtime.Debug;
import client.requests.runtime.Execute;
import client.responses.info.FunctionsResponder;
import client.responses.info.ProgramByNameResponder;
import client.responses.runtime.*;
import display.*;
import execution.ExecutionDTO;
import execution.ExecutionPollDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import application.execution.header.HeaderController;
import application.execution.inputs.InputsController;
import application.execution.outputs.OutputsController;
import application.execution.run.options.RunOptionsController;
import application.execution.summary.SummaryController;
import application.execution.table.instruction.InstructionsController;
import javafx.stage.Stage;
import okhttp3.Request;
import types.LabelDTO;
import types.VarOptionsDTO;
import types.VarRefDTO;
import utils.Constants;
import utils.ExecTarget;

public class ExecutionSceneController {
    @FXML private ScrollPane rootScroll;
    @FXML private VBox contentRoot;
    @FXML private HeaderController          headerController;
    @FXML private InstructionsController    programTableController;
    @FXML private SummaryController         summaryController;
    @FXML private InstructionsController    chainTableController;
    @FXML private RunOptionsController      runOptionsController;
    @FXML private OutputsController         outputsController;
    @FXML private InputsController          inputsController;
    @FXML private ComboBox<String> cmbArchitecture;
    @FXML private Button btnBackToOpening;

    private Consumer<String> onArchitectureSelected;
    private String userName;
    private ExecTarget targetKind;
    private String targetName;
    private int maxDegree;
    private int currentDegree = 0;
    private ExpandDTO lastExpanded = null;
    private String currentHighlight;
    private DisplayDTO display;

    // --- Debug session state (server-driven) ---
    private volatile String       debugId            = null;
    private volatile boolean      debugActive        = false;
    private volatile boolean      debugStopRequested = false;
    private volatile DebugStateDTO lastDebugState    = null;
    private Thread                debugPoller        = null;
    private boolean debugMode    = false; // user chose "Debug"
    private boolean debugStarted = false; // init completed
    private Thread resumeWatcher = null;

    private static final Pattern X_VAR_PATTERN = Pattern.compile("\\bx(\\d+)\\b");
    private static final Pattern Z_VAR_PATTERN = Pattern.compile("\\bz(\\d+)\\b");

    @FXML
    private void initialize() {
        rootScroll.setFitToWidth(false);
        rootScroll.setFitToHeight(false);
        contentRoot.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        contentRoot.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        DoubleBinding viewportW = Bindings.selectDouble(rootScroll.viewportBoundsProperty(), "width");
        DoubleBinding viewportH = Bindings.selectDouble(rootScroll.viewportBoundsProperty(), "height");
        contentRoot.translateXProperty().bind(
                Bindings.max(0, viewportW.subtract(contentRoot.widthProperty()).divide(2))
        );
        contentRoot.translateYProperty().bind(
                Bindings.max(0, viewportH.subtract(contentRoot.heightProperty()).divide(2))
        );

        if (cmbArchitecture != null) {
            cmbArchitecture.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (onArchitectureSelected != null) onArchitectureSelected.accept(nv);
            });
            cmbArchitecture.getItems().setAll("I","II","III","IV");
        }
    }

    @FXML
    private void onBackToOpening() {
        try {
            openOpeningAndReplace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void init(ExecTarget target, String name, int maxDegree) {
        targetKind = target;
        targetName = name;
        this.maxDegree  = Math.max(0, maxDegree);

        // 1) Header title + degrees
        if (headerController != null) {
            String prefix = (target == ExecTarget.PROGRAM) ? "Program: " : "Function: ";
            headerController.setRunTarget(prefix + name);
            headerController.setMaxDegree(this.maxDegree);
            headerController.setCurrentDegree(0);
            headerController.setOnExpand(()  -> changeDegreeAndShow(+1));
            headerController.setOnCollapse(() -> changeDegreeAndShow(-1));
            headerController.setOnApplyDegree(() -> doApply(headerController.getCurrentDegree()));
            headerController.setOnHighlightChanged(sel -> {
                currentHighlight = ("NONE".equals(sel) || "Highlight selection".equals(sel)) ? null : sel;
                if (programTableController != null && programTableController.getTableView() != null)
                    programTableController.getTableView().refresh();
                if (chainTableController != null && chainTableController.getTableView() != null)
                    chainTableController.getTableView().refresh();
            });
            headerController.refreshStatus();
        }

        // 2) Summary follows the main program table
        if (summaryController != null && programTableController != null) {
            summaryController.wireTo(programTableController);
            this.setOnArchitectureSelected(arch -> {
                summaryController.setSelectedArchitecture(arch);
                executeGenerationCheck();
            });
        }

        if (runOptionsController != null) {
            runOptionsController.setMainController(this);
        }

        if (programTableController != null && chainTableController != null) {
            chainTableController.hideLineColumn();
            programTableController.selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                List<InstructionDTO> chain = programTableController.getCreatedByChainFor(newSel);
                chainTableController.show(chain == null ? List.of() : chain);
            });
        }

        // 3) Load DisplayDTO off the FX thread
        new Thread(() -> {
            try {
                DisplayDTO dto = (target == ExecTarget.PROGRAM)
                        ? ProgramByNameResponder.execute(name)    // by program name
                        : FunctionsResponder.program(name);      // by function user-string (key)

                if (dto == null) return;
                Platform.runLater(() -> {
                    this.display = dto;
                    // degree 0 flat instructions into the main table
                    programTableController.show(dto.getInstructions());
                    // === HIGHLIGHT: after first render of degree 0 ===
                    if (headerController != null
                            && programTableController != null
                            && programTableController.getTableView() != null) {
                        headerController.populateHighlight(
                                programTableController.getTableView().getItems(), true);
                        wireHighlight(programTableController);
                        currentHighlight = headerController.getSelectedHighlight();
                        programTableController.getTableView().refresh();
                        if (chainTableController != null && chainTableController.getTableView() != null) {
                            chainTableController.getTableView().refresh();
                        }
                    }
                    // chain table stays empty for now (we'll use it on expand/debug)
                    if (chainTableController != null) {
                        chainTableController.show(List.of());
                    }
                    if (runOptionsController != null) {
                        runOptionsController.startEnabled(true);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "exec-load-" + ((target == ExecTarget.PROGRAM) ? "program" : "function")).start();
    }

    private void changeDegreeAndShow(int i) {
        doApply(currentDegree + i);
    }

    private void doApply(int requestedDegree) {
        int target = Math.max(0, Math.min(requestedDegree, maxDegree));
        currentDegree = target;
        if (headerController != null) { headerController.setCurrentDegree(currentDegree); }
        if (inputsController != null) {
            inputsController.clear();
            inputsController.setInputsEditable(false);
        }

        new Thread(() -> {
            try {
                ExpandDTO dto = (targetKind == ExecTarget.PROGRAM)
                        ? ExpandResponder.execute(target)
                        : ExpandResponder.execute(targetName, target);
                Platform.runLater(() -> {
                    lastExpanded = dto;
                    if (programTableController != null) {
                        programTableController.showExpanded(dto);
                        executeGenerationCheck();
                    }
                    if (headerController != null && programTableController != null && programTableController.getTableView() != null) {
                        headerController.populateHighlight(
                                programTableController.getTableView().getItems(), /*resetToNone=*/true);
                        currentHighlight = null;
                        programTableController.getTableView().refresh();
                        if (chainTableController != null && chainTableController.getTableView() != null) {
                            chainTableController.getTableView().refresh();
                        }
                    }
                });
            } catch (Exception ignore) {
                // TODO: לוג/שגיאה עדינה אם תרצי
            }
        }, "expand-" + target).start();
    }

    // =====================================================================================
    // v2 entry point: single button that decides RUN vs DEBUG based on debugMode.
    // Call this from RunOptionsController's Execute button (recommended).
    // =====================================================================================
    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }
        if (display == null || inputsController == null) return;

        if (debugMode) {
            ensureDebugInit(); // will pull the first state immediately and enable buttons
            return;
        }

        // Regular run:
        executeRun();
    }

    public void executeRun() {
        if (headerController == null || outputsController == null) return;
        List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : List.of();

        final int degree = headerController.getCurrentDegree();
        final ExecutionRequestDTO dto = new ExecutionRequestDTO(degree, inputs);
        final String functionUserString = (targetKind == ExecTarget.FUNCTION) ? targetName : null;
        outputsController.clear();

        new Thread(() -> {
            try {
                // 1) build the original POST (we reuse it to extract the correct /api/execute URL)
                Request submitReq = Execute.build(dto, functionUserString);
                String executeUrl = submitReq.url().toString();
                String jobId;
                while (true) {
                    JobSubmitResult sr = ExecuteResponder.submit(
                            ExecuteResponder.buildSubmitRequest(executeUrl, dto, functionUserString)
                    );
                    if (sr.isAccepted()) {
                        jobId = sr.getJobId();
                        break;
                    }
                    try { Thread.sleep(Math.max(200, sr.getRetryMs())); } catch (InterruptedException ie) { return; }
                }

                // 3) POLL until terminal state
                ExecutionDTO result = null;
                String errorMsg = null;

                while (true) {
                    Request pollReq = ExecuteResponder.buildPollRequest(executeUrl, jobId);
                    ExecutionPollDTO pr = ExecuteResponder.poll(pollReq);

                    switch (pr.getStatus()) {
                        case PENDING:
                        case RUNNING:
                            try { Thread.sleep(300); } catch (InterruptedException ignore) {}
                            continue;
                        case DONE:
                            result = pr.getResult();
                            break;
                        case CANCELED:
                            errorMsg = "Canceled";
                            break;
                        case TIMED_OUT:
                            errorMsg = "Timed out";
                            break;
                        case ERROR:
                        default:
                            errorMsg = (pr.getError() == null || pr.getError().isBlank())
                                    ? "Unknown error"
                                    : pr.getError();
                            break;
                    }
                    break;
                }

                if (result != null) {
                    final ExecutionDTO finalResult = result;
                    Platform.runLater(() -> {
                        outputsController.showExecution(finalResult);
                        if (inputsController != null) {
                            inputsController.setInputsEditable(false);
                        }
                    });
                } else {
                    final String em = errorMsg;
                    Platform.runLater(() ->
                            showError("Execution failed", em == null ? "Unknown error" : em));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        showError("Execution failed", ex.getMessage()));
            }
        }, "execute-run").start();
    }

    /** Toggle “debug mode” UI-wise (called by RunOptionsController). */
    public void setDebugMode(boolean on) {
        this.debugMode = on; // <— keep v2 flag in sync

        if (!on) {
            // Leaving debug mode – restore UI to normal
            stopDebugPoller();
            debugId = null;
            debugActive = false;
            debugStopRequested = false;
            debugStarted = false;
            lastDebugState = null;

            if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(true);
            if (inputsController != null) inputsController.setInputsEditable(true);
            if (programTableController != null) {
                programTableController.setHighlightPredicate(i -> false);
                if (programTableController.getTableView() != null) {
                    programTableController.getTableView().getSelectionModel().clearSelection();
                    if (programTableController.getTableView().getFocusModel() != null)
                        programTableController.getTableView().getFocusModel().focus(-1);
                }
            }
        }
    }

    private void ensureDebugInit() {
        if (debugStarted || !debugMode) {
            if (lastDebugState != null) {
                Platform.runLater(() -> showDebugState(lastDebugState));
            }
            return;
        }
        // Reuse the existing startDebug() to perform the init.
        startDebug();
    }

    public void startDebug() {
        if (headerController == null || outputsController == null) return;

        final List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : List.of();
        final int degree = headerController.getCurrentDegree();
        final ExecutionRequestDTO dto = new ExecutionRequestDTO(degree, inputs);
        final String functionUserString = (targetKind == ExecTarget.FUNCTION) ? targetName : null;

        outputsController.clear();

        new Thread(() -> {
            try {
                // 1) Initialize debug session (with simple retry while busy)
                String id;
                while (true) {
                    DebugResults.Submit res = DebugResponder.init(Debug.init(dto, functionUserString));
                    if (res.accepted()) { id = res.debugId(); break; }
                    if (res.locked()) {
                        showError("Debug locked",
                                "This program/function is currently locked by another session.\nPlease try again later.");
                        return;
                    }
                    try { Thread.sleep(Math.max(300, res.retryMs())); } catch (InterruptedException ie) { return; }
                }
                debugId = id;
                debugActive = true;
                debugStarted = true;
                debugStopRequested = false;

                // 2) Try to pull the first state (may be null for a brief moment) — retry a little.
                DebugStateDTO first = null;
                for (int i = 0; i < 10 && first == null; i++) {
                    try {
                        first = DebugResponder.state(Debug.state(id));
                    } catch (Exception ignore) { /* transient */ }
                    if (first == null) {
                        try { Thread.sleep(100); } catch (InterruptedException ie) { return; }
                    }
                }
                lastDebugState = first; // may still be null; that's OK

                // 3) Update UI (single runLater, null-safe)
                final DebugStateDTO firstState = first;
                Platform.runLater(() -> {
                    if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(false);
                    if (inputsController != null) inputsController.setInputsEditable(false);

                    // Use plain table selection (no highlight predicate)
                    if (programTableController != null) {
                        programTableController.setHighlightPredicate(i -> false);
                        if (programTableController.getTableView() != null) {
                            programTableController.getTableView().refresh();
                        }
                    }

                    // If we already have a first state, render it and select its PC
                    if (firstState != null) {
                        showDebugState(firstState);
                        selectAndScrollProgramRow(firstState.getPc());
                        if (outputsController != null) outputsController.highlightChanged(Set.of());
                    } else {
                        // No state yet: clear previous highlights; user can Step/Resume to proceed
                        if (outputsController != null) outputsController.highlightChanged(Set.of());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Debug error", ex.getMessage());
            }
        }, "dbg-init").start();
    }

    /** Resume from current pause and behave like a regular run:
     *  - no state() polling while running (avoids starving the run)
     *  - poll only terminated()
     *  - on finish, fetch one final state and update UI
     */
    public void debugResume() {
        final String id = debugId;
        if (!debugActive || id == null) {
            ensureDebugInit();
            return;
        }
        // We don't want the state() poller during resume – it can block the run.
        stopDebugPoller();
        stopResumeWatcher();
        // Clear red/bold step highlight when switching to continuous run
        if (outputsController != null) {
            Platform.runLater(() -> outputsController.highlightChanged(Set.of()));
        }
        debugStopRequested = false;
        resumeWatcher = new Thread(() -> {
            try {
                // 1) send resume (retry a bit if server is busy)
                int attempts = 0;
                while (attempts < 5) {
                    DebugResults.Submit res = DebugResponder.resume(Debug.resume(id));
                    if (res.accepted()) break;
                    try { Thread.sleep(Math.max(300, res.retryMs())); } catch (InterruptedException ie) { return; }
                    attempts++;
                }

                // 2) wait until terminated(), without calling state() in a loop
                while (debugActive && !debugStopRequested) {
                    DebugResults.Terminated done = DebugResponder.terminated(Debug.terminated(id));
                    if (done != null && done.terminated()) break;
                    try { Thread.sleep(Math.max(300, Constants.REFRESH_RATE_MS)); } catch (InterruptedException ie) { return; }
                }

                // 3) one final snapshot at the very end (execute-like)
                DebugStateDTO finalState;
                try {
                    finalState = DebugResponder.state(Debug.state(id));
                } catch (Exception ignore) {
                    finalState = lastDebugState; // fallback if state endpoint already closed
                }
                final DebugStateDTO fs = finalState;

                Platform.runLater(() -> {
                    showDebugState(fs);                             // update outputs + cycles
                    if (outputsController != null) outputsController.highlightChanged(Set.of());
                    onDebugTerminated(fs);                          // local cleanup; DO NOT call stop() (avoids 404)
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError("Resume failed", ex.getMessage()));
            }
        }, "dbg-resume-waiter");

        resumeWatcher.setDaemon(true);
        resumeWatcher.start();
    }

    public void debugStep() {
        final String id = debugId;
        if (!debugActive || id == null) return;

        new Thread(() -> {
            try {
                DebugStepDTO step = DebugResponder.step(Debug.step(id));
                if (step != null && step.getNewState() != null) {
                    DebugStateDTO st = step.getNewState();

                    // Compute "changed" BEFORE replacing lastDebugState
                    final Set<String> changed = diffChangedNames(lastDebugState, st);

                    Platform.runLater(() -> {
                        // 1) render fresh outputs/cycles
                        showDebugState(st);
                        // 2) clear old highlight, then apply only the changed vars
                        if (outputsController != null) {
                            outputsController.highlightChanged(Set.of());
                            outputsController.highlightChanged(changed);
                        }
                        // 3) select/scroll using v2 logic (handles jumps)
                        rowToHighlight(lastDebugState, st);
                    });

                    lastDebugState = st;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Step failed", ex.getMessage());
            }
        }, "dbg-step").start();
    }

    /** Stop the debug session on server and clean up UI. */
    public void debugStop() {
        final String id = debugId;
        if (id == null) return;
        debugStopRequested = true;
        stopResumeWatcher();
        new Thread(() -> {
            try {
                DebugResults.Stop res = DebugResponder.stop(Debug.stop(id));
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Stop failed", ex.getMessage());
            } finally {
                // UI cleanup
                debugActive = false;
                debugId = null;
                stopDebugPoller();
                Platform.runLater(() -> {
                    if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(true);
                    if (inputsController   != null) inputsController.setInputsEditable(true);
                    if (programTableController != null) {
                        programTableController.setHighlightPredicate(i -> false);
                        if (programTableController.getTableView() != null) {
                            programTableController.getTableView().getSelectionModel().clearSelection();
                            if (programTableController.getTableView().getFocusModel() != null)
                                programTableController.getTableView().getFocusModel().focus(-1);
                        }
                    }
                });
            }
        }, "dbg-stop").start();
    }

    /** Poll current state during resume; update outputs/cycles live; finalize cleanly on termination. */
    private void startDebugPoller() {
        stopDebugPoller();
        final String id = debugId;
        if (id == null) return;

        debugPoller = new Thread(() -> {
            while (debugActive && !debugStopRequested) {
                try {
                    // Live state (variables + cycles)
                    DebugStateDTO st = null;
                    try { st = DebugResponder.state(Debug.state(id)); } catch (Exception ignore) {}
                    if (st != null) {
                        final DebugStateDTO curr = st;
                        Platform.runLater(() -> {
                            // No auto-scroll during resume; user can scroll freely
                            showDebugState(curr);
                            if (outputsController != null) outputsController.highlightChanged(Set.of());
                        });
                        lastDebugState = curr;
                    }

                    // Termination check
                    DebugResults.Terminated done = null;
                    try { done = DebugResponder.terminated(Debug.terminated(id)); } catch (Exception ignore) {}
                    if (done != null && done.terminated()) {
                        DebugStateDTO finalState;
                        try { finalState = DebugResponder.state(Debug.state(id)); } catch (Exception ignore) { finalState = lastDebugState; }
                        final DebugStateDTO fs = finalState;
                        Platform.runLater(() -> {
                            // Show final state and clean up (no stop() call -> no 404)
                            showDebugState(fs);
                            if (outputsController != null) outputsController.highlightChanged(Set.of());
                            onDebugTerminated(fs);
                        });
                        break;
                    }

                    Thread.sleep(Math.max(150, Constants.REFRESH_RATE_MS));
                } catch (InterruptedException ie) {
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Debug polling error", ex.getMessage());
                    return;
                }
            }
        }, "dbg-poll");
        debugPoller.setDaemon(true);
        debugPoller.start();
    }


    private void stopDebugPoller() {
        if (debugPoller != null) {
            try { debugPoller.interrupt(); } catch (Throwable ignore) {}
            debugPoller = null;
        }
    }

    /** Render a DebugStateDTO onto the outputs (no table selection/scroll here). */
    private void showDebugState(DebugStateDTO state) {
        if (state == null) return;
        // Cycles
        if (outputsController != null) {
            outputsController.setCycles(state.getCyclesSoFar());
        }
        // Build a full variable list (y, all x..., all z...) from program refs + args + current state
        Set<Integer> xs = new TreeSet<>();
        Set<Integer> zs = new TreeSet<>();

        if (programTableController != null && programTableController.getTableView() != null) {
            for (InstructionDTO ins : programTableController.getTableView().getItems()) {
                InstructionBodyDTO body = (ins != null) ? ins.getBody() : null;
                if (body == null) continue;
                for (VarRefDTO r : new VarRefDTO[]{
                        body.getVariable(), body.getDest(), body.getSource(),
                        body.getCompare(), body.getCompareWith()
                }) {
                    if (r == null) continue;
                    switch (r.getVariable()) {
                        case x -> xs.add(r.getIndex());
                        case z -> zs.add(r.getIndex());
                        case y -> {}
                    }
                }
                if (body.getOp() == InstrOpDTO.QUOTE || body.getOp() == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    parseVariablesFromArgs(body.getFunctionArgs(), xs, zs);
                }
            }
        }

        Map<String, Long> values = new HashMap<>();
        if (state.getVars() != null) {
            for (VarValueDTO v : state.getVars()) {
                switch (v.getVar().getVariable()) {
                    case x -> { xs.add(v.getVar().getIndex()); values.put("x" + v.getVar().getIndex(), v.getValue()); }
                    case z -> { zs.add(v.getVar().getIndex()); values.put("z" + v.getVar().getIndex(), v.getValue()); }
                    case y -> values.put("y", v.getValue());
                }
            }
        }
        List<String> lines = new ArrayList<>(1 + xs.size() + zs.size());
        lines.add("y = " + values.getOrDefault("y", 0L));
        for (int i : xs) lines.add("x" + i + " = " + values.getOrDefault("x" + i, 0L));
        for (int i : zs) lines.add("z" + i + " = " + values.getOrDefault("z" + i, 0L));

        if (outputsController != null) {
            outputsController.setVariableLines(lines);
            // Do NOT call highlightChanged here; callers (step/resume/stop) decide when to highlight.
        }
    }

    /** Finalize a terminated debug session locally, without calling stop() on server. */
    private void onDebugTerminated(DebugStateDTO finalState) {
        debugActive = false;
        debugId = null;
        stopDebugPoller();

        if (finalState != null) {
            showDebugState(finalState);
            if (outputsController != null) outputsController.highlightChanged(java.util.Set.of());
            lastDebugState = finalState;
        }

        if (runOptionsController != null) {
            runOptionsController.setDebugBtnsDisabled(true);
            runOptionsController.startEnabled(true);
            runOptionsController.clearRunCheckBox();
        }

        // לא נוגעים ב-inputs כאן; אם תרצי – הפכי אותם ללא-עריכים רק כשהדיבאג פעיל.
    }

    private void stopResumeWatcher() {
        if (resumeWatcher != null) {
            try { resumeWatcher.interrupt(); } catch (Throwable ignore) {}
            resumeWatcher = null;
        }
    }

    private void openOpeningAndReplace() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/opening/opening_scene.fxml"));
        Parent root = loader.load();
        OpeningSceneController opening = loader.getController();
        if (userName != null) opening.setUserName(userName);
        Stage stage = (Stage) rootScroll.getScene().getWindow();
        stage.setTitle("S-emulator");
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void wireHighlight(InstructionsController ic) {
        if (ic == null || ic.getTableView() == null) return;
        ic.setHighlightPredicate(ins -> {
            if (ins == null) return false;
            String sel = currentHighlight;
            if (sel == null || sel.isBlank() || "Highlight selection".equals(sel)) return false;
            InstructionBodyDTO body = ins.getBody();
            LabelDTO label = ins.getLabel();
            LabelDTO targetLabel = (body != null) ? body.getJumpTo() : null;
            if ("EXIT".equals(sel)) {
                return (targetLabel != null && targetLabel.isExit());
            }
            if (sel.startsWith("L")) {
                boolean isLabelMatch  = (label != null && !label.isExit() && sel.equals(label.getName()));
                boolean isJumpToMatch = (targetLabel != null && !targetLabel.isExit() && sel.equals(targetLabel.getName()));
                return isLabelMatch || isJumpToMatch;
            }
            if (body != null) {
                for (VarRefDTO var : new VarRefDTO[]{
                        body.getVariable(), body.getDest(), body.getSource(), body.getCompare(), body.getCompareWith()
                }) {
                    if (var == null) continue;
                    switch (var.getVariable()) {
                        case y -> { if ("y".equals(sel)) return true; }
                        case x -> { if (sel.equals("x" + var.getIndex())) return true; }
                        case z -> { if (sel.equals("z" + var.getIndex())) return true; }
                    }
                }
                if (body.getOp() == InstrOpDTO.QUOTE || body.getOp() == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    String args = body.getFunctionArgs();
                    if (args != null && !args.isBlank()) {
                        if (sel.startsWith("x") && args.matches(".*\\bx" + sel.substring(1) + "\\b.*")) return true;
                        if (sel.startsWith("z") && args.matches(".*\\bz" + sel.substring(1) + "\\b.*")) return true;
                    }
                }
            }
            return false;
        });
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        inputsController.show(display);
        inputsController.setInputsEditable(true);
        Platform.runLater(inputsController::focusFirstField);
    }

    // ===== API לשימוש האפליקציה בהמשך =====
    public void setArchitectureOptions(List<String> options) {
        cmbArchitecture.getItems().setAll(options);
    }
    public void selectArchitecture(String value) {
        cmbArchitecture.getSelectionModel().select(value);
    }
    public String getSelectedArchitecture() {
        return cmbArchitecture.getSelectionModel().getSelectedItem();
    }

    public void setOnBackToOpening(Runnable cb) { /* reserved for future use */ }
    public void setOnArchitectureSelected(Consumer<String> cb) { this.onArchitectureSelected = cb; }
    public void setUserName(String name) {
        this.userName = name;
        if (headerController != null) headerController.setUserName(name);
    }

    // ****helpers****
    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            TextArea area = new TextArea(msg);
            area.setEditable(false);
            area.setWrapText(true);
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        });
    }

    /** Pretty-print variables in the required order for the outputs box. */
    private static List<String> formatVarsForDisplay(List<VarValueDTO> vars) {
        if (vars == null) return List.of();
        // Map name->value, then sort by y/x/z and index
        Map<String, Long> map = new HashMap<>();
        for (VarValueDTO vv : vars) {
            map.put(varName(vv.getVar()), vv.getValue());
        }
        List<String> names = map.keySet().stream().sorted((a, b) -> {
            // y first, then x1..xn, then z1..zn
            int ra = rank(a), rb = rank(b);
            if (ra != rb) return Integer.compare(ra, rb);
            return Integer.compare(extractIndex(a), extractIndex(b));
        }).collect(Collectors.toList());
        List<String> lines = new ArrayList<>(names.size());
        for (String n : names) {
            lines.add(n + " = " + map.get(n));
        }
        return lines;
    }

    private static int rank(String n) {
        if ("y".equals(n)) return 0;
        if (n.startsWith("x")) return 1;
        return 2; // z*
    }
    private static int extractIndex(String n) {
        if (n.length() <= 1) return 0;
        try { return Integer.parseInt(n.substring(1)); } catch (Exception ignore) { return 0; }
    }
    private static String varName(VarRefDTO ref) {
        VarOptionsDTO v = ref.getVariable();
        if (v == VarOptionsDTO.y) return "y";
        int idx = Math.max(1, ref.getIndex());
        return (v == VarOptionsDTO.x ? "x" : "z") + idx;
    }

    /** Set of variable names that changed between two snapshots (for red highlight). */
    private static Set<String> diffChangedNames(DebugStateDTO prev, DebugStateDTO curr) {
        if (prev == null || curr == null) return Set.of();
        Map<String, Long> a = toMap(prev.getVars());
        Map<String, Long> b = toMap(curr.getVars());
        return b.entrySet().stream()
                .filter(e -> !a.containsKey(e.getKey()) || !a.get(e.getKey()).equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static Map<String, Long> toMap(List<VarValueDTO> vars) {
        Map<String, Long> m = new HashMap<>();
        if (vars != null) for (VarValueDTO v : vars) m.put(varName(v.getVar()), v.getValue());
        return m;
    }

    private static void parseVariablesFromArgs(String text, Set<Integer> xs, Set<Integer> zs) {
        if (text == null || text.isBlank()) return;
        Matcher mx = X_VAR_PATTERN.matcher(text);
        while (mx.find()) { try { xs.add(Integer.parseInt(mx.group(1))); } catch (Exception ignore) {} }
        Matcher mz = Z_VAR_PATTERN.matcher(text);
        while (mz.find()) { try { zs.add(Integer.parseInt(mz.group(1))); } catch (Exception ignore) {} }
    }

    /** Select and scroll the main program table to the given PC (row index), if valid. */
    private void selectAndScrollProgramRow(int pc) {
        if (programTableController == null || programTableController.getTableView() == null) return;
        TableView<InstructionDTO> tv = programTableController.getTableView();
        int n = tv.getItems().size();
        if (n == 0) return;

        if (pc < 0 || pc >= n) {
            tv.getSelectionModel().clearSelection();
            if (tv.getFocusModel() != null) tv.getFocusModel().focus(-1);
            return;
        }
        tv.getSelectionModel().clearAndSelect(pc);
        if (tv.getFocusModel() != null) tv.getFocusModel().focus(pc);
        tv.scrollTo(pc);
    }

    /**
     * If control flow jumped (not linear pc+1), try to select the target label row.
     * Otherwise select the state's pc.
     */
    private void rowToHighlight(DebugStateDTO prev, DebugStateDTO state) {
        int targetIndex = (state != null) ? state.getPc() : -1;

        if (prev != null
                && programTableController != null
                && programTableController.getTableView() != null
                && programTableController.getTableView().getItems() != null) {

            List<InstructionDTO> items = programTableController.getTableView().getItems();
            int prevPc = prev.getPc();
            int newPc  = targetIndex;
            boolean controlTransfer = (newPc != prevPc + 1);

            if (controlTransfer && prevPc >= 0 && prevPc < items.size()) {
                InstructionDTO prevIns = items.get(prevPc);
                InstructionBodyDTO body = (prevIns != null) ? prevIns.getBody() : null;
                LabelDTO tgt = (body != null) ? body.getJumpTo() : null;

                if (tgt != null && !tgt.isExit()) {
                    String want = tgt.getName();
                    if (want != null && !want.isBlank()) {
                        for (int i = 0; i < items.size(); i++) {
                            LabelDTO lbl = items.get(i).getLabel();
                            if (lbl != null && !lbl.isExit() && want.equals(lbl.getName())) {
                                targetIndex = i;
                                break;
                            }
                        }
                    }
                }
            }
        }
        selectAndScrollProgramRow(targetIndex);
    }

    private int convertRomanToInteger(String v) {
        if (v == null) return 0;
        String t = v.trim().toUpperCase();
        return switch (t) { case "I" -> 1; case "II" -> 2; case "III" -> 3; case "IV" -> 4; default -> 0; };
    }

    private void executeGenerationCheck() {
        if (runOptionsController == null || programTableController == null) return;
        String sel = getSelectedArchitecture();
        String maxInTable = programTableController.getMaxGenerationValue();
        int selRank = convertRomanToInteger(sel);
        int maxRank = convertRomanToInteger(maxInTable);
        boolean shouldDisable = sel != null && selRank > 0 && maxRank > 0 && (selRank < maxRank);
        runOptionsController.startEnabled(!shouldDisable);
    }
}
