package application;

import api.DebugAPI;
import application.table.history.RowSnapshot;
import display.*;
import execution.*;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.application.Platform;

import application.format.ExecutionFormatter;
import application.header.HeaderController;
import application.table.instruction.InstructionsController;
import application.summary.SummaryController;
import application.table.history.HistoryController;
import application.run.options.RunOptionsController;
import application.outputs.OutputsController;
import application.inputs.InputsController;

import api.DisplayAPI;
import api.ExecutionAPI;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import types.LabelDTO;
import display.InstructionBodyDTO;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramSceneController {
    @FXML private HeaderController headerController;
    @FXML private InstructionsController programTableController;
    @FXML private SummaryController summaryController;
    @FXML private InstructionsController chainTableController;
    @FXML private RunOptionsController runOptionsController;
    @FXML private OutputsController outputsController;
    @FXML private InputsController inputsController;
    @FXML private HistoryController historyController;
    @FXML private ScrollPane rootScroll;
    @FXML private VBox contentRoot;

    private DisplayAPI display;
    private DisplayAPI rootDisplay;
    private Map<String, DisplayAPI> functionDisplays = Collections.emptyMap();
    private ExecutionAPI execute;
    private int currentDegree = 0;
    private DebugAPI debugApi = null;
    private boolean debugMode = false;
    private boolean debugStarted = false;
    private volatile boolean debugStopRequested = false;
    private Task<Void> debugResumeTask = null;
    private DebugStateDTO lastDebugState = null;
    private final Map<String, List<RunHistoryEntryDTO>> uiHistoryByKey = new LinkedHashMap<>();
    private final Map<String, RowSnapshot> snapshots = new LinkedHashMap<>();
    private String currentHistoryKey = null;

    private String currentHighlight = null;
    private static final String HIGHLIGHT_CLASS = "hilite";
    private static final Pattern X_VAR_PATTERN = Pattern.compile("\\bx(\\d+)\\b");
    private static final Pattern Z_VAR_PATTERN = Pattern.compile("\\bz(\\d+)\\b");

    @FXML
    private void initialize() {
        if (runOptionsController != null) {
            runOptionsController.setMainController(this);
        }

        if (headerController != null) {
            headerController.setOnLoaded(this::onProgramLoaded);
            headerController.setOnProgramSelected(this::onProgramComboChanged);
            headerController.setOnExpand(()  -> changeDegreeAndShow(+1));
            headerController.setOnCollapse(() -> changeDegreeAndShow(-1));
            headerController.setOnSkinChanged(this::applySkin);
            headerController.setOnApplyDegree(() -> doApply(headerController.getCurrentDegree()));

            headerController.setOnHighlightChanged(sel -> {
                currentHighlight = ("NONE".equals(sel) ? null : sel);
                if (programTableController != null && programTableController.getTableView() != null)
                    programTableController.getTableView().refresh();
                if (chainTableController != null && chainTableController.getTableView() != null)
                    chainTableController.getTableView().refresh();
            });

            headerController.setOnAnimationsChanged(enabled -> {
                if (historyController != null) historyController.setAnimationsEnabled(enabled);
                if (programTableController != null) programTableController.setAnimationsEnabled(enabled);
            });
        }

        if (summaryController != null && programTableController != null) {
            summaryController.wireTo(programTableController);
        }

        if (programTableController != null && chainTableController != null) {
            wireAncestry();
        }

        if (programTableController != null) {
            programTableController.showLineColumn();
            programTableController.setAnimationsEnabled(headerController.isAnimationsOn());
        }
        if (chainTableController != null) {
            chainTableController.hideLineColumn();
        }

        if (historyController != null) {
            historyController.setOnRerun(this::onHistoryRerun);
            historyController.setOnShow(this::onHistoryShow);
            historyController.setAnimationsEnabled(headerController.isAnimationsOn());
        }

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

        if (programTableController != null
                && programTableController.getTableView() != null
                && headerController != null) {

            programTableController.getTableView()
                    .getItems()
                    .addListener((ListChangeListener<InstructionDTO>) change -> {
                        if (display != null) {
                            DisplayDTO cmd2 = display.getCommand2();
                            headerController.populateHighlight(cmd2.getInstructions());
                        }
                    });
        }
        wireHighlight(programTableController);

        // שימי לב: לא נועלים/משנים את ה־Run/Debug כשיש BP. הבחירה נשארת אצל המשתמש.
        // אם תרצי חיווי קל כשיש BP (ללא נעילה) אפשר להוסיף כאן בהמשך.
    }

    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.rootDisplay = display;
        this.functionDisplays = display.functionDisplaysByUserString();
        this.execute = display.execution();
        debugApi = null;
        debugMode = false;
        debugStarted = false;
        debugStopRequested = false;
        debugResumeTask = null;

        uiHistoryByKey.clear();
        snapshots.clear();
        currentHistoryKey = null;

        if (inputsController != null)  inputsController.clear();
        if (outputsController != null) outputsController.clear();
        if (historyController != null) historyController.clear();

        if (chainTableController != null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true);
        }

        int maxDegree = execute.getMaxDegree();
        currentDegree = 0;
        headerController.setMaxDegree(maxDegree);
        headerController.setCurrentDegree(currentDegree);

        if (programTableController != null) {
            ExpandDTO expanded = this.display.expand(currentDegree);
            programTableController.showExpanded(expanded);
            if (programTableController != null) programTableController.clearBreakpoint();
            if (headerController != null && programTableController != null && programTableController.getTableView() != null) {
                headerController.populateHighlight(programTableController.getTableView().getItems(), true);
                wireHighlight(programTableController);
                currentHighlight = headerController.getSelectedHighlight();
                programTableController.getTableView().refresh();
            }
        }
        if (headerController != null) {
            headerController.populateProgramFunction(display, true);
            headerController.setOnProgramSelected(this::onProgramComboChanged);
        }

        String firstKey = (headerController != null) ? headerController.getSelectedProgramFunction() : null;
        if (firstKey == null && display.getCommand2() != null) {
            firstKey = "PROGRAM: " + display.getCommand2().getProgramName();
        }
        if (firstKey != null) {
            applyHistoryForKey(firstKey);
        }

        if (runOptionsController != null) {
            runOptionsController.startEnabled(true);
            runOptionsController.setButtonsEnabled(false);
        }
        updateChain(programTableController != null ? programTableController.getSelectedItem() : null);
    }

    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }
        if (display == null || inputsController == null) return;

        // מצב Debug נבחר ע"י המשתמש (דרך הצ'קבוקס)
        if (debugMode) {
            // התחלה ראשונית של Debug:
            if (!debugStarted) {
                Integer bpPc = (programTableController != null) ? programTableController.getBreakpointPc() : null;
                if (bpPc != null) {
                    // לרוץ "חלק" עד ה־BP ואז להיכנס לדיבאג שם
                    inputsController.setInputsEditable(false);
                    runUntilBreakpointThenEnterDebug(bpPc);
                    return;
                } else {
                    // אין BP → דיבאג רגיל מהשורה הראשונה
                    ensureDebugInit();
                    if (lastDebugState != null) selectAndScrollProgramRow(lastDebugState.getPc());
                    return;
                }
            }
            // כבר בתוך דיבאג — שומרים את ההתנהגות הקיימת
            ensureDebugInit();
            if (lastDebugState != null) selectAndScrollProgramRow(lastDebugState.getPc());
            return;
        }

        // מצב Run רגיל
        String csv = inputsController.collectValuesCsvPadded();
        if (outputsController != null) outputsController.setVariableLines(java.util.List.of(csv));
        handleRun();
        inputsController.setInputsEditable(false);

        // לפי הבקשה הקודמת: לנקות BP אחרי כל ריצה
        clearBreakpointOnly();
    }

    private void handleRun() {
        if (display == null) return;

        List<Long> inputs = inputsController.collectValuesPadded();
        int degree = headerController.getCurrentDegree();

        execute = display.executionForDegree(degree);
        ExecutionRequestDTO req = new ExecutionRequestDTO(degree, inputs);
        ExecutionDTO result = execute.execute(req);

        outputsController.showExecution(result);

        HistoryDTO history = display.getHistory();
        List<RunHistoryEntryDTO> entries = (history != null) ? history.getEntries() : null;
        if (historyController != null && entries != null && !entries.isEmpty()) {
            RunHistoryEntryDTO last = new RunHistoryEntryDTO(
                    historyController.getTableSize()+1, degree,
                    inputs, result.getyValue(), (int) result.getTotalCycles()
            );
            historyController.addEntry(last);
            if (currentHistoryKey != null) {
                snapshots.put(snapKey(currentHistoryKey, last.getRunNumber()), RowSnapshot.ofExec(result));
            }
        }
    }

    private void changeDegreeAndShow(int i) {
        doApply(currentDegree + i);
    }

    private void doApply(int requestedDegree) {
        if (display == null || programTableController == null) return;

        if (execute == null) {
            execute = display.execution();
        }
        int max = (execute != null) ? execute.getMaxDegree() : 0;
        int target = Math.max(0, Math.min(requestedDegree, max));

        if (headerController != null && headerController.isAnimationsOn()) {
            programTableController.requestPopulateAnimation(true);
        }
        ExpandDTO expanded = display.expand(target);
        programTableController.showExpanded(expanded);
        if (programTableController != null) programTableController.clearBreakpoint();
        if (headerController != null && programTableController.getTableView() != null) {
            headerController.populateHighlight(programTableController.getTableView().getItems());
            wireHighlight(programTableController);
            currentHighlight = headerController.getSelectedHighlight();
            programTableController.getTableView().refresh();
        }

        if (programTableController.getTableView() != null) {
            programTableController.getTableView().getSelectionModel().clearSelection();
        }
        if (outputsController != null) {
            outputsController.clear();
        }

        debugStarted = false;
        debugApi = null;
        debugStopRequested = false;
        if (debugResumeTask != null) {
            debugResumeTask.cancel();
            debugResumeTask = null;
        }

        currentDegree = target;
        if (headerController != null) {
            headerController.setCurrentDegree(currentDegree);
        }

        if (runOptionsController != null) {
            runOptionsController.startEnabled(true);
            runOptionsController.setButtonsEnabled(false);
        }

        updateChain(programTableController.getSelectedItem());
    }

    public void setDebugMode(boolean on) {
        this.debugMode = on;
        if (!on) {
            debugApi = null;
            debugStarted = false;
            debugStopRequested = false;
            debugResumeTask = null;
        }
    }

    private void ensureDebugInit() {
        if (debugStarted || !debugMode || display == null) return;
        List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : Collections.emptyList();

        int degree = (headerController != null) ? headerController.getCurrentDegree() : 0;
        debugApi = display.debugForDegree(degree);
        DebugStateDTO state = debugApi.init(new ExecutionRequestDTO(degree, inputs));
        debugStarted = true;
        if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(false);
        lastDebugState = state;
        showDebugState(state);
        if (inputsController != null) inputsController.setInputsEditable(false);
        outputsController.highlightChanged(Set.of());
    }

    public void debugStep() {
        if (!debugMode) return;
        ensureDebugInit();
        if (debugApi == null) return;
        DebugStateDTO prev = lastDebugState;
        DebugStepDTO step = debugApi.step();
        DebugStateDTO state = step.getNewState();
        Set<String> changed = computeChangedVarNames(prev, state);

        lastDebugState = state;
        showDebugState(state);
        outputsController.highlightChanged(changed);
        rowToHighlight(prev, state);

        if (debugApi.isTerminated() && lastDebugState != null && historyController != null) {
            if (outputsController != null) outputsController.highlightChanged(Set.of());
            debugToHistory();
            if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(true);
            debugMode = false;
            runOptionsController.startEnabled(true);
            runOptionsController.setButtonsEnabled(false);
            runOptionsController.clearRunCheckBox();

            if (programTableController != null && programTableController.getTableView() != null) {
                TableView<InstructionDTO> tv = programTableController.getTableView();
                tv.getSelectionModel().clearSelection();
                if (tv.getFocusModel() != null) tv.getFocusModel().focus(-1);
            }
            clearBreakpointOnly();
        }
    }

    private void rowToHighlight(DebugStateDTO prev, DebugStateDTO state) {
        int targetIndex = (state != null) ? state.getPc() : -1;
        if (prev != null && programTableController != null && programTableController.getTableView() != null && !debugApi.isTerminated()) {
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

    public void debugResume() {
        if (!debugMode) return;
        ensureDebugInit();
        if (debugApi == null || (debugResumeTask != null && debugResumeTask.isRunning())) return;

        debugStopRequested = false;
        if (runOptionsController != null) runOptionsController.setResumeBusy(true);

        debugResumeTask = new Task<>() {
            @Override
            protected Void call() {
                while (!isCancelled() && !debugStopRequested) {
                    DebugStepDTO step  = debugApi.step();
                    DebugStateDTO state = step.getNewState();
                    DebugStateDTO prev = lastDebugState;
                    lastDebugState = state;

                    Platform.runLater(() -> {
                        showDebugState(state);
                        outputsController.highlightChanged(computeChangedVarNames(prev, state));

                        rowToHighlight(prev, state);
                        if (debugApi != null && debugApi.isTerminated() && outputsController != null) {
                            outputsController.highlightChanged(Set.of());
                        }
                    });

                    if (debugApi.isTerminated()) break;
                }
                return null;
            }
        };

        debugResumeTask.setOnSucceeded(e -> {
            if (runOptionsController != null) runOptionsController.setResumeBusy(false);
            if (!debugStopRequested && debugApi != null && debugApi.isTerminated() &&
                    lastDebugState != null && historyController != null) {
                if (outputsController != null) outputsController.highlightChanged(Set.of());

                debugToHistory();
                if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(true);
                debugMode = false;
                runOptionsController.startEnabled(true);
                runOptionsController.setButtonsEnabled(false);
                runOptionsController.clearRunCheckBox();

                if (programTableController != null && programTableController.getTableView() != null) {
                    TableView<InstructionDTO> tv = programTableController.getTableView();
                    tv.getSelectionModel().clearSelection();
                    if (tv.getFocusModel() != null) tv.getFocusModel().focus(-1);
                }
                clearBreakpointOnly(); // לנקות BP בסיום
            }
        });
        debugResumeTask.setOnCancelled(e -> { if (runOptionsController != null) runOptionsController.setResumeBusy(false); });
        debugResumeTask.setOnFailed(e -> { if (runOptionsController != null) runOptionsController.setResumeBusy(false); });

        Thread t = new Thread(debugResumeTask, "debug-resume");
        t.setDaemon(true);
        t.start();
    }

    public void debugStop() {
        debugStopRequested = true;
        if (debugResumeTask != null) debugResumeTask.cancel();
        if (runOptionsController != null) runOptionsController.setResumeBusy(false);
        if (lastDebugState != null && historyController != null) {

            if (outputsController != null) outputsController.highlightChanged(Set.of());
            debugToHistory();
            if (runOptionsController != null) runOptionsController.setDebugBtnsDisabled(true);
            debugMode = false;
            if (programTableController != null && programTableController.getTableView() != null) {
                TableView<InstructionDTO> tv = programTableController.getTableView();
                tv.getSelectionModel().clearSelection();
                if (tv.getFocusModel() != null) tv.getFocusModel().focus(-1);
            }
        }
        clearBreakpointOnly();
        runOptionsController.startEnabled(true);
        runOptionsController.setButtonsEnabled(false);
        runOptionsController.clearRunCheckBox();
    }

    private void debugToHistory() {
        List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : Collections.emptyList();
        int degree = (headerController != null) ? headerController.getCurrentDegree() : 0;
        int entryIndex = historyController.getTableSize() + 1;
        historyController.addEntry(lastDebugState, degree, inputs);
        if (currentHistoryKey != null && outputsController != null) {
            String sk = snapKey(currentHistoryKey, entryIndex);
            snapshots.merge(sk, RowSnapshot.ofDebug(outputsController.getVariableLines()), RowSnapshot::merge);
        }
        if (inputsController != null) inputsController.setInputsEditable(false);
    }

    private void showDebugState(DebugStateDTO state) {
        if (state == null || outputsController == null) return;
        outputsController.setCycles(state.getCyclesSoFar());
        Set<Integer> xs = new TreeSet<>();
        Set<Integer> zs = new TreeSet<>();

        if (programTableController != null && programTableController.getTableView() != null) {
            for (InstructionDTO ins : programTableController.getTableView().getItems()) {
                InstructionBodyDTO body = (ins != null) ? ins.getBody() : null;
                if (body == null) continue;

                for (types.VarRefDTO r : new types.VarRefDTO[]{
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
                    String argsText = body.getFunctionArgs();
                    parseVariablesFromArgs(argsText, xs, zs);
                }
            }
        }

        Map<String, Long> values = new HashMap<>();
        for (VarValueDTO v : state.getVars()) {
            switch (v.getVar().getVariable()) {
                case x -> {
                    xs.add(v.getVar().getIndex());
                    values.put("x" + v.getVar().getIndex(), v.getValue());
                }
                case z -> {
                    zs.add(v.getVar().getIndex());
                    values.put("z" + v.getVar().getIndex(), v.getValue());
                }
                case y -> values.put("y", v.getValue());
            }
        }

        List<String> lines = new ArrayList<>(1 + xs.size() + zs.size());
        lines.add("y = " + values.getOrDefault("y", 0L));
        for (int i : xs) lines.add("x" + i + " = " + values.getOrDefault("x" + i, 0L));
        for (int i : zs) lines.add("z" + i + " = " + values.getOrDefault("z" + i, 0L));

        outputsController.setVariableLines(lines);
    }

    private void runUntilBreakpointThenEnterDebug(int bpPc) {
        java.util.List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : java.util.Collections.emptyList();
        int degree = (headerController != null) ? headerController.getCurrentDegree() : 0;

        DebugAPI localDebug = display.debugForDegree(degree);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                DebugStateDTO state = localDebug.init(new ExecutionRequestDTO(degree, inputs));
                while (!localDebug.isTerminated() && state.getPc() != bpPc) {
                    state = localDebug.step().getNewState();
                }
                DebugStateDTO reached = state;

                Platform.runLater(() -> {
                    // נגמר לפני שהגענו ל-BP: מציגים תוצאה רגילה ומנקים BP
                    if (localDebug.isTerminated() && reached.getPc() != bpPc) {
                        handleRun(); // להציג outputs/history בפורמט הרגיל
                        clearBreakpointOnly();
                        showBreakpointMessage();
                        return;
                    }

                    // אימוץ סשן הדיבאג והכניסה למצב דיבאג החל מה-BP
                    debugApi = localDebug;
                    lastDebugState = reached;
                    debugStarted = true;
                    setDebugMode(true);

                    showDebugState(reached);
                    outputsController.highlightChanged(Set.of());
                    selectAndScrollProgramRow(reached.getPc());

                    if (runOptionsController != null) {
                        runOptionsController.setDebugBtnsDisabled(false); // מפעיל Resume/Step/Stop
                    }
                });
                return null;
            }
        };
        Thread t = new Thread(task, "bp-runner");
        t.setDaemon(true);
        t.start();
    }

    private static void parseVariablesFromArgs(String text, Set<Integer> xs, Set<Integer> zs) {
        if (text == null || text.isBlank()) return;
        Matcher mx = X_VAR_PATTERN.matcher(text);
        while (mx.find()) {
            xs.add(Integer.parseInt(mx.group(1)));
        }
        Matcher mz = Z_VAR_PATTERN.matcher(text);
        while (mz.find()) {
            zs.add(Integer.parseInt(mz.group(1)));
        }
    }

    private static Set<String> computeChangedVarNames(DebugStateDTO prev, DebugStateDTO curr) {
        Set<String> changed = new HashSet<>();
        if (curr == null) return changed;
        Map<String, Long> prevMap = new HashMap<>();
        Map<String, Long> currMap = new HashMap<>();

        if (prev != null) {
            runOnVars(prev, prevMap);
        }
        runOnVars(curr, currMap);
        Set<String> keys = new HashSet<>(currMap.keySet());
        keys.addAll(prevMap.keySet());
        for (String k : keys) {
            long oldVal = prevMap.getOrDefault(k, 0L);
            long newVal = currMap.getOrDefault(k, 0L);
            if (oldVal != newVal) changed.add(k);
        }
        return changed;
    }

    private static void runOnVars(DebugStateDTO prev, Map<String, Long> prevMap) {
        for (VarValueDTO v : prev.getVars()) {
            String name = switch (v.getVar().getVariable()) {
                case y -> "y";
                case x -> "x" + v.getVar().getIndex();
                case z -> "z" + v.getVar().getIndex();
            };
            prevMap.put(name, v.getValue());
        }
    }

    private void selectAndScrollProgramRow(int pc) {
        if (programTableController == null || programTableController.getTableView() == null) return;
        TableView<InstructionDTO> programTableView = programTableController.getTableView();
        int n = programTableView.getItems().size();
        if (n == 0) return;
        if (pc < 0 || pc >= n) {
            programTableView.getSelectionModel().clearSelection();
            return;
        }

        programTableView.getSelectionModel().clearAndSelect(pc);
        programTableView.getFocusModel().focus(pc);
        programTableView.scrollTo(pc);
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        DisplayDTO dto = display.getCommand2();
        inputsController.show(dto);
        Platform.runLater(inputsController::focusFirstField);
    }

    private void onProgramComboChanged(String sel) {
        if (programTableController == null || headerController == null) return;
        if (debugMode && debugStarted) {
            debugStop();
        }
        String key = (sel != null) ? sel : headerController.getSelectedProgramFunction();
        runOptionsController.clearRunCheckBox();

        if (key == null || !key.startsWith("FUNCTION: ")) {
            this.display = rootDisplay;
            this.execute = display.execution();
            headerController.setMaxDegree(execute.getMaxDegree());
            ExpandDTO expanded = display.expand(currentDegree);
            programTableController.showExpanded(expanded);

            // ניקוי BP
            if (programTableController != null) programTableController.clearBreakpoint();

            fillComboboxes();
            updateChain(programTableController.getSelectedItem());
            applyHistoryForKey(key);
            return;
        }

        String userStr = key.substring("FUNCTION: ".length()).trim();
        DisplayAPI fnDisplay = functionDisplays.get(userStr);
        if (fnDisplay == null) {
            onProgramComboChanged(null);
            return;
        }

        this.display = fnDisplay;
        this.execute = display.execution();
        currentDegree = 0;
        headerController.setCurrentDegree(currentDegree);
        headerController.setMaxDegree(execute.getMaxDegree());

        ExpandDTO expanded = display.expand(currentDegree);
        programTableController.showExpanded(expanded);

        if (chainTableController != null) {
            chainTableController.clear();
            if (chainTableController.getTableView() != null) {
                chainTableController.getTableView().setDisable(true);
            }
        }

        // ניקוי BP
        if (programTableController != null) programTableController.clearBreakpoint();

        fillComboboxes();
        applyHistoryForKey(key);
    }

    private void fillComboboxes() {
        if (inputsController != null)  inputsController.clear();
        if (outputsController != null) outputsController.clear();

        if (programTableController.getTableView() != null) {
            headerController.populateHighlight(programTableController.getTableView().getItems(), true);
            wireHighlight(programTableController);
            currentHighlight = headerController.getSelectedHighlight();
            programTableController.getTableView().refresh();
        }
    }

    private void onHistoryRerun(RunHistoryEntryDTO row) {
        if (row == null || display == null || programTableController == null || headerController == null || inputsController == null) return;

        currentDegree = row.getDegree();
        headerController.setCurrentDegree(currentDegree);
        ExpandDTO expanded = display.expand(currentDegree);
        programTableController.showExpanded(expanded);
        updateChain(programTableController.getSelectedItem());

        inputsController.show(display.getCommand2());
        inputsController.fillInputs(row.getInputs());
        Platform.runLater(inputsController::focusFirstField);

        if (outputsController != null) {
            outputsController.clear();
        }

        if (runOptionsController != null) {
            runOptionsController.startEnabled(false);
            runOptionsController.setButtonsEnabled(true);
        }
    }

    private void onHistoryShow(RunHistoryEntryDTO row) {
        if (row == null) return;
        int runNo = row.getRunNumber();
        String sk = snapKey(currentHistoryKey, runNo);
        RowSnapshot s = snapshots.get(sk);

        if (s != null && s.hasExec()) {
            openTextPopup("Run #" + runNo + " ", buildRunText(row, s.getExec()));
        }
        else if (s != null && s.hasDebugText()) {
            openTextPopup("Run #" + runNo + " ", buildRunText(row, s.getDebugText()));
        }
    }

    private String buildRunText(RunHistoryEntryDTO row, ExecutionDTO snap) {
        StringBuilder str = new StringBuilder();

        str.append("Run #").append(row.getRunNumber())
                .append(" | Degree: ").append(row.getDegree())
                .append("\n");

        str.append("y = ").append(row.getYValue());
        if (snap != null && snap.getFinals() != null && !snap.getFinals().isEmpty()) {
            str.append("\n").append(ExecutionFormatter.formatAllVars(snap.getFinals())).append("\n");
        }
        return str.toString();
    }

    private String buildRunText(RunHistoryEntryDTO row, List<String> snap) {
        StringBuilder str = new StringBuilder();

        str.append("Run #").append(row.getRunNumber())
                .append(" | Degree: ").append(row.getDegree())
                .append("\n");

        for (String s : snap) {
            str.append(s);
            str.append("\n");
        }
        return str.toString();
    }

    private void openTextPopup(String title, String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initOwner(rootScroll.getScene().getWindow());
        stage.initModality(Modality.NONE);
        stage.setScene(new Scene(area, 250, 200));
        stage.show();
    }

    // טוסט לא חוסם במרכז החלון
    private void showBreakpointMessage() {
        Popup popup = new Popup();
        Label msg = new Label("Run finished — breakpoint not reached");
        msg.setStyle(
                "-fx-background-color: rgba(40,40,40,0.92);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 10;" +
                        "-fx-font-size: 12px;"
        );
        msg.setMouseTransparent(true);
        msg.setOpacity(0);

        popup.getContent().add(msg);

        var win = rootScroll.getScene().getWindow();
        // מציגים ואז מרכזים לפי הגודל האמיתי של התוכן
        popup.show(win);
        Platform.runLater(() -> {
            double x = win.getX() + (win.getWidth()  - msg.getWidth())  / 2.0;
            double y = win.getY() + (win.getHeight() - msg.getHeight()) / 2.0;
            popup.setX(x);
            popup.setY(y);

            var fadeIn = new FadeTransition(Duration.millis(150), msg);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            var stay = new PauseTransition(Duration.seconds(1.7));

            var fadeOut = new FadeTransition(Duration.millis(220), msg);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            var seq = new SequentialTransition(fadeIn, stay, fadeOut);
            seq.setOnFinished(e -> popup.hide());
            seq.play();
        });
    }

    private void wireAncestry() {
        updateChain(programTableController.getSelectedItem());
        programTableController.selectedItemProperty().addListener((obs, oldSel, newSel) -> updateChain(newSel));
    }

    private void updateChain(InstructionDTO selected) {
        if (chainTableController == null) return;

        if (selected == null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true);
            return;
        }
        List<InstructionDTO> ancestry = new ArrayList<>();
        ancestry.add(selected);
        ancestry.addAll(programTableController.getCreatedByChainFor(selected));
        Collections.reverse(ancestry);
        chainTableController.setRows(ancestry);
        chainTableController.getTableView().setDisable(ancestry.isEmpty());
    }

    private void wireHighlight(InstructionsController ic) {
        if (ic == null || ic.getTableView() == null) return;

        ic.setHighlightPredicate(ins -> {
            if (ins == null) return false;
            String selected = currentHighlight;
            if (selected == null || selected.isBlank()) return false;

            InstructionBodyDTO body = ins.getBody();
            LabelDTO label = ins.getLabel();
            LabelDTO targetLabel = (body != null) ? body.getJumpTo() : null;

            if ("EXIT".equals(selected)) {
                return (targetLabel != null && targetLabel.isExit());
            } else if (selected.startsWith("L")) {
                boolean isLabelMatch  = (label != null && !label.isExit() && selected.equals(label.getName()));
                boolean isJumpToMatch = (targetLabel != null && !targetLabel.isExit() && selected.equals(targetLabel.getName()));
                return isLabelMatch || isJumpToMatch;
            } else if (body != null) {
                for (types.VarRefDTO var : new types.VarRefDTO[]{
                        body.getVariable(),
                        body.getDest(),
                        body.getSource(),
                        body.getCompare(),
                        body.getCompareWith()
                }) {
                    if (var == null) continue;
                    types.VarOptionsDTO k = var.getVariable();
                    int idx = var.getIndex();
                    if ("y".equals(selected) && k == types.VarOptionsDTO.y) return true;
                    if (k == types.VarOptionsDTO.x && selected.equals("x" + idx)) return true;
                    if (k == types.VarOptionsDTO.z && selected.equals("z" + idx)) return true;
                }
                if (body.getOp() == InstrOpDTO.QUOTE || body.getOp() == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    String argsText = body.getFunctionArgs();
                    if (argsContainSelectedVar(selected, argsText)) return true;
                }
            }
            return false;
        });
    }

    private void applyHistoryForKey(String key) {
        if (historyController == null) return;
        if (currentHistoryKey != null && historyController.getTableView() != null) {
            uiHistoryByKey.put(currentHistoryKey,
                    new ArrayList<>(historyController.getTableView().getItems()));
        }
        currentHistoryKey = key;
        historyController.clear();

        List<RunHistoryEntryDTO> rows = uiHistoryByKey.computeIfAbsent(key, k -> new ArrayList<>());
        for (RunHistoryEntryDTO r : rows) {
            historyController.addEntry(r);
        }
    }

    private static String snapKey(String key, int runNo) {
        return key + "|" + runNo;
    }

    // bonus
    public void applySkin(String skinClass) {
        ObservableList<String> classes = rootScroll.getStyleClass();
        classes.removeIf(s -> s.startsWith("skin-"));

        // clear selections to avoid color conflicts
        if (programTableController != null && programTableController.getTableView() != null) {
            programTableController.getTableView().getSelectionModel().clearSelection();
        }
        if (chainTableController != null && chainTableController.getTableView() != null) {
            chainTableController.getTableView().getSelectionModel().clearSelection();
        }
        if (historyController != null && historyController.getTableView() != null) {
            historyController.getTableView().getSelectionModel().clearSelection();
        }

        if (skinClass != null && !skinClass.isBlank() && !classes.contains(skinClass)) {
            classes.add(skinClass);
        }
        rootScroll.applyCss();
    }

    private static boolean argsContainSelectedVar(String selected, String argsText) {
        if (argsText == null || selected == null || selected.isBlank()) return false;
        java.util.regex.Pattern p =
                java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(selected) + "\\b");
        return p.matcher(argsText).find();
    }

    // מנקה רק את ה-BP (בלי לגעת ב-toolbar)
    private void clearBreakpointOnly() {
        if (programTableController != null) programTableController.clearBreakpoint();
    }
}
