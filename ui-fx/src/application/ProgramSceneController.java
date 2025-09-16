package application;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
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

import display.Command2DTO;
import display.Command3DTO;
import display.InstructionDTO;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.HistoryDTO;
import execution.RunHistoryEntryDTO;

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

    private String currentHighlight = null;
    private static final String HILITE_CLASS = "hilite";


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
                currentHighlight = sel;
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
                            Command2DTO cmd2 = display.getCommand2();
                            headerController.populateHighlight(cmd2.getInstructions());
                        }
                    });
        }

        wireHighlight(programTableController);
    }


    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }

        if (display == null || inputsController == null) return;

        String csv = inputsController.collectValuesCsvPadded();
        if (outputsController != null) {
            outputsController.setVariableLines(List.of("Inputs: " + csv));
        }

        handleRun();
    }


    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.execute = display.execution();

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
            Command3DTO expanded = this.display.expand(currentDegree);
            programTableController.showExpanded(expanded);
            if (headerController != null && programTableController != null && programTableController.getTableView() != null) {
                headerController.populateHighlight(programTableController.getTableView().getItems());
            }


        }

        if (runOptionsController != null) {
            runOptionsController.startEnabled(true);
            runOptionsController.setButtonsEnabled(false);


        }



        updateChain(programTableController != null ? programTableController.getSelectedItem() : null);
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
            RunHistoryEntryDTO last = entries.getLast();
            historyController.addEntry(last);
            runSnapshots.put(last.getRunNumber(), result);
        }

    }

    private void changeDegreeAndShow(int i) {
        if (display == null || programTableController == null) return;

        int target = currentDegree + i;

        Command3DTO expanded = display.expand(target);
        programTableController.showExpanded(expanded);
        if (headerController != null && programTableController != null && programTableController.getTableView() != null) {
            headerController.populateHighlight(programTableController.getTableView().getItems());
        }

        currentDegree = target;
        assert headerController != null;
        headerController.setCurrentDegree(currentDegree);


        updateChain(programTableController.getSelectedItem());
    }


    public void setDisplay(DisplayAPI display) {
        this.display = display;
        this.execute = null;
    }

    public void showCommand2(Command2DTO dto) {
        if (programTableController != null) {
            programTableController.show(dto);
            updateChain(null);
            if (headerController != null && dto != null) {
                headerController.populateHighlight(dto.getInstructions());
            }

        }
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        Command2DTO dto = display.getCommand2();
        inputsController.show(dto);
        Platform.runLater(inputsController::focusFirstField);


    }

    private void onHistoryRerun(execution.RunHistoryEntryDTO row) {
        if (row == null || display == null || programTableController == null || headerController == null) return;

        currentDegree = row.getDegree();
        headerController.setCurrentDegree(currentDegree);
        display.Command3DTO expanded = display.expand(currentDegree);
        programTableController.showExpanded(expanded);
        updateChain(programTableController.getSelectedItem());
        inputsController.fillInputs(row.getInputs());
        if (outputsController != null) {
            outputsController.clear();
        }



    }

    private void onHistoryShow(RunHistoryEntryDTO row) {
        if (row == null) return;
        ExecutionDTO snap = runSnapshots.get(row.getRunNumber());
        String text = buildRunText(row, snap);
        openTextPopup("Run #" + row.getRunNumber() + " ", text);
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

                boolean on = false;
                String sel = currentHighlight;

                if (!empty && ins != null && sel != null && !sel.isBlank()) {
                    var body = ins.getBody();

                    // 1) בחירה היא EXIT → שורה מודגשת אם jumpTo הוא EXIT
                    if ("EXIT".equals(sel)) {
                        if (body != null) {
                            LabelDTO jt = (LabelDTO) body.getJumpTo();
                            on = (jt != null && jt.isExit());
                        }
                    }
                    // 2) בחירה היא Label L# → שורה מודגשת אם label.name == sel
                    else if (sel.startsWith("L")) {
                        LabelDTO lbl = (LabelDTO) ins.getLabel();
                        on = (lbl != null && !lbl.isExit() && sel.equals(lbl.getName()));
                    }
                    // 3) בחירה היא משתנה (y / xN / zN) → מודגשת אם מופיע באחד משדות הגוף
                    else if (body != null) {
                        VarRefDTO[] refs = new VarRefDTO[] {
                                (VarRefDTO) body.getVariable(),
                                (VarRefDTO) body.getDest(),
                                (VarRefDTO) body.getSource(),
                                (VarRefDTO) body.getCompare(),
                                (VarRefDTO) body.getCompareWith()
                        };
                        for (VarRefDTO r : refs) {
                            if (r == null) continue;
                            VarOptionsDTO k = r.getVariable();
                            int idx = r.getIndex();

                            if ("y".equals(sel) && k == VarOptionsDTO.y) { on = true; break; }
                            if (k == VarOptionsDTO.x && sel.equals("x" + idx)) { on = true; break; }
                            if (k == VarOptionsDTO.z && sel.equals("z" + idx)) { on = true; break; }
                        }
                    }
                }

                getStyleClass().remove(HILITE_CLASS);
                if (on) getStyleClass().add(HILITE_CLASS);

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
