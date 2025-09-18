package application;

import api.DebugAPI;
import execution.*;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.application.Platform;

import format.ExecutionFormatter;
import application.header.HeaderController;
import application.table.instruction.InstructionsController;
import application.summary.SummaryController;
import application.table.history.HistoryController;
import application.run.options.RunOptionsController;
import application.outputs.OutputsController;
import application.inputs.InputsController;

import api.DisplayAPI;
import api.ExecutionAPI;

import display.DisplayDTO;
import display.ExpandDTO;
import display.InstructionDTO;

import javafx.scene.Scene;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import types.LabelDTO;
import types.VarOptionsDTO;
import types.VarRefDTO;

import java.util.*;

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
    private ExecutionAPI execute;
    private int currentDegree = 0;

    private final Map<Integer, ExecutionDTO> runSnapshots = new HashMap<>();
    private final Map<Integer, List<String>> debugSnapshots = new HashMap<>();

    private DebugAPI debugApi = null;
    private boolean debugMode = false;
    private boolean debugStarted = false;
    private volatile boolean debugStopRequested = false;
    private Task<Void> debugResumeTask = null;
    private DebugStateDTO lastDebugState = null;

    private String currentHighlight = null;
    private static final String HIGHLIGHT_CLASS = "hilite";

    @FXML
    private void initialize() {
        if (runOptionsController != null) {
            runOptionsController.setMainController(this);
        }

        if (headerController != null) {
            headerController.setOnLoaded(this::onProgramLoaded);
            headerController.setOnExpand(()  -> changeDegreeAndShow(+1));
            headerController.setOnCollapse(() -> changeDegreeAndShow(-1));
            headerController.setOnThemeChanged(this::applyTheme);

            headerController.setOnHighlightChanged(sel -> {
                currentHighlight = ("NONE".equals(sel) ? null : sel);
                if (programTableController != null && programTableController.getTableView() != null)
                    programTableController.getTableView().refresh();
                if (chainTableController != null && chainTableController.getTableView() != null)
                    chainTableController.getTableView().refresh();
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
        }
        if (chainTableController != null) {
            chainTableController.hideLineColumn();
        }

        if (historyController != null) {
            historyController.setOnRerun(this::onHistoryRerun);
            historyController.setOnShow(this::onHistoryShow);
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
    }

    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.execute = display.execution();
        debugApi = null;
        debugMode = false;
        debugStarted = false;
        debugStopRequested = false;
        debugResumeTask = null;

        if (inputsController != null)  inputsController.clear();
        if (outputsController != null) outputsController.clear();
        if (historyController != null) historyController.clear();

        runSnapshots.clear();

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
            if (headerController != null && programTableController != null && programTableController.getTableView() != null) {
                headerController.populateHighlight(programTableController.getTableView().getItems(), true); // ← לאפס ל-NONE
                wireHighlight(programTableController);
                currentHighlight = headerController.getSelectedHighlight();
                programTableController.getTableView().refresh();
            }
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

        if (debugMode) {
            ensureDebugInit();
            if (lastDebugState != null) {
                selectAndScrollProgramRow(lastDebugState.getPc());
            }
            return;
        }


        String csv = inputsController.collectValuesCsvPadded();
        if (outputsController != null) {
            outputsController.setVariableLines(List.of(csv));
        }
        handleRun();
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
            runSnapshots.put(last.getRunNumber(), result);
        }
    }

    private void changeDegreeAndShow(int i) {
        if (display == null || programTableController == null) return;
        int target = currentDegree + i;

        ExpandDTO expanded = display.expand(target);
        programTableController.showExpanded(expanded);

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
        headerController.setCurrentDegree(currentDegree);
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
        lastDebugState = state;
        showDebugState(state);
        outputsController.highlightChanged(Set.of());
    }

    public void debugStep() {
        if (!debugMode) return;

        ensureDebugInit();

        if (debugApi == null) return;

        DebugStepDTO step = debugApi.step();
        DebugStateDTO state = step.getNewState();
        Set<String> changed = computeChangedVarNames(lastDebugState, state);

        lastDebugState = state;
        showDebugState(state);
        outputsController.highlightChanged(changed);
        selectAndScrollProgramRow(state.getPc());
        if (debugApi.isTerminated() && lastDebugState != null && historyController != null) {
            debugToHistory();
            runOptionsController.setDebugBtnsDisabled(true);
        }

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
                    var step  = debugApi.step();
                    var state = step.getNewState();
                    var prev = lastDebugState;
                    lastDebugState = state;

                    Platform.runLater(() -> {
                        showDebugState(state);
                        outputsController.highlightChanged(computeChangedVarNames(prev, state));
                        selectAndScrollProgramRow(state.getPc());
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
                debugToHistory();
                runOptionsController.setDebugBtnsDisabled(true);
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
            debugToHistory();
            runOptionsController.setDebugBtnsDisabled(true);
        }
    }

    private void debugToHistory() {
        List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : Collections.emptyList();
        int degree = (headerController != null) ? headerController.getCurrentDegree() : 0;
        int entryIndex = historyController.getTableSize() + 1;
        debugSnapshots.put(entryIndex, outputsController.getVariableLines());
        historyController.addEntry(lastDebugState, degree, inputs);
    }


    private void showDebugState(DebugStateDTO state) {
        if (state == null || outputsController == null) return;
        outputsController.setCycles(state.getCyclesSoFar());

        Set<String> names = new LinkedHashSet<>();
        names.add("y");
        if (programTableController != null && programTableController.getTableView() != null) {
            Set<Integer> xs = new TreeSet<>();
            Set<Integer> zs = new TreeSet<>();
            for (display.InstructionDTO ins : programTableController.getTableView().getItems()) {
                var body = (ins != null) ? ins.getBody() : null;
                if (body == null) continue;
                for (types.VarRefDTO r : new types.VarRefDTO[]{
                      body.getVariable(),
                      body.getDest(),
                      body.getSource(),
                      body.getCompare(),
                      body.getCompareWith()
                }) {
                    if (r == null) continue;
                    switch (r.getVariable()) {
                        case x -> xs.add(r.getIndex());
                        case z -> zs.add(r.getIndex());
                        case y -> {}
                    }
                }
            }
            xs.forEach(i -> names.add("x" + i));
            zs.forEach(i -> names.add("z" + i));
        }

        Map<String, Long> values = new HashMap<>();
        for (VarValueDTO v : state.getVars()) {
            String n = switch (v.getVar().getVariable()) {
                case y -> "y";
                case x -> "x" + v.getVar().getIndex();
                case z -> "z" + v.getVar().getIndex();
            };
            values.put(n, v.getValue());
        }

        List<String> lines = new ArrayList<>(names.size());
        for (String n : names) {
            long val = values.getOrDefault(n, 0L);
            lines.add(n + " = " + val);
        }
        outputsController.setVariableLines(lines);
    }

    private static Set<String> computeChangedVarNames(DebugStateDTO prev, DebugStateDTO curr) {
        Set<String> changed = new HashSet<>();
        if (curr == null) return changed;
        Map<String, Long> pm = new HashMap<>();
        Map<String, Long> cm = new HashMap<>();

        if (prev != null) {
            for (VarValueDTO v : prev.getVars()) {
                String name = switch (v.getVar().getVariable()) {
                    case y -> "y";
                    case x -> "x" + v.getVar().getIndex();
                    case z -> "z" + v.getVar().getIndex();
                };
                pm.put(name, v.getValue());
            }
        }
        for (VarValueDTO v : curr.getVars()) {
            String name = switch (v.getVar().getVariable()) {
                case y -> "y";
                case x -> "x" + v.getVar().getIndex();
                case z -> "z" + v.getVar().getIndex();
            };
            cm.put(name, v.getValue());
        }

        Set<String> keys = new HashSet<>(cm.keySet());
        keys.addAll(pm.keySet());
        for (String k : keys) {
            long oldVal = pm.getOrDefault(k, 0L);
            long newVal = cm.getOrDefault(k, 0L);
            if (oldVal != newVal) changed.add(k);
        }
        return changed;
    }

    private void selectAndScrollProgramRow(int pc) {
        if (programTableController == null || programTableController.getTableView() == null) return;
        var tv = programTableController.getTableView();
        int n = tv.getItems().size();
        if (n == 0) return;
        if (pc < 0 || pc >= n) {
            tv.getSelectionModel().clearSelection();
            return;
        }

        tv.getSelectionModel().clearAndSelect(pc);
        tv.getFocusModel().focus(pc);
        tv.scrollTo(pc);
    }


    public void setDisplay(DisplayAPI display) {
        this.display = display;
        this.execute = null;
    }

    public void showCommand2(DisplayDTO dto) {
        if (programTableController != null) {
            programTableController.show(dto);
            updateChain(null);
            if (headerController != null && dto != null) {
                headerController.populateHighlight(dto.getInstructions());
                wireHighlight(programTableController);
                currentHighlight = headerController.getSelectedHighlight();
                programTableController.getTableView().refresh();
            }
        }
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        DisplayDTO dto = display.getCommand2();
        inputsController.show(dto);
        Platform.runLater(inputsController::focusFirstField);
    }

    private void onHistoryRerun(RunHistoryEntryDTO row) {
        if (row == null || display == null || programTableController == null || headerController == null) return;

        currentDegree = row.getDegree();
        headerController.setCurrentDegree(currentDegree);
        ExpandDTO expanded = display.expand(currentDegree);
        programTableController.showExpanded(expanded);
        updateChain(programTableController.getSelectedItem());
        inputsController.fillInputs(row.getInputs());
        if (outputsController != null) {
            outputsController.clear();
        }
    }

    private void onHistoryShow(RunHistoryEntryDTO row) {
        if (row == null) return;
        if(runSnapshots.containsKey(row.getRunNumber()))
        {
            ExecutionDTO snap = runSnapshots.get(row.getRunNumber());
            String text = buildRunText(row, snap);
            openTextPopup("Run #" + row.getRunNumber() + " ", text);
        }
        else {
            List<String> snap = debugSnapshots.get(row.getRunNumber());
            String text = buildRunText(row, snap);
            openTextPopup("Run #" + row.getRunNumber() + " ", text);
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
        ic.getTableView().setRowFactory(tv -> new TableRow<InstructionDTO>() {
            @Override
            protected void updateItem(InstructionDTO ins, boolean empty) {
                super.updateItem(ins, empty);

                if (empty || ins == null) {
                    getStyleClass().removeAll(HIGHLIGHT_CLASS);
                    return;
                }

                boolean on = false;
                String sel = currentHighlight;

                if (sel != null && !sel.isBlank()) {
                    var body    = ins.getBody();
                    LabelDTO my = (LabelDTO) ins.getLabel();
                    LabelDTO jt = (body != null) ? (LabelDTO) body.getJumpTo() : null;

                    if ("EXIT".equals(sel)) {
                        on = (jt != null && jt.isExit());
                    } else if (sel.startsWith("L")) {
                        boolean isMy = (my != null && !my.isExit() && sel.equals(my.getName()));
                        boolean j2L  = (jt != null && !jt.isExit() && sel.equals(jt.getName()));
                        on = isMy || j2L; // גם שורת התווית וגם מי שקופץ אליה
                    } else if (body != null) {
                        for (VarRefDTO r : new VarRefDTO[]{
                               body.getVariable(),
                               body.getDest(),
                               body.getSource(),
                               body.getCompare(),
                               body.getCompareWith()
                        }) {
                            if (r == null) continue;
                            VarOptionsDTO k = r.getVariable();
                            int idx = r.getIndex();
                            if ("y".equals(sel) && k == VarOptionsDTO.y) { on = true; break; }
                            if (k == VarOptionsDTO.x && sel.equals("x" + idx)) { on = true; break; }
                            if (k == VarOptionsDTO.z && sel.equals("z" + idx)) { on = true; break; }
                        }
                    }
                }

                getStyleClass().removeAll(HIGHLIGHT_CLASS);
                if (on) getStyleClass().add(HIGHLIGHT_CLASS);
            }
        });
    }

    //bonus
    public void applyTheme(String themeClass) {
        ObservableList<String> classes = rootScroll.getStyleClass();
        classes.removeIf(s -> s.startsWith("theme-"));

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

        if (themeClass != null && !themeClass.isBlank() && !classes.contains(themeClass)) {
            classes.add(themeClass);
        }
        rootScroll.applyCss();
    }
}
