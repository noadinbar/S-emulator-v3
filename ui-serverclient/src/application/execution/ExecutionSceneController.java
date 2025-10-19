package application.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import application.opening.OpeningSceneController;
import client.requests.Execute;
import client.responses.ExecuteResponder;
import client.responses.ExpandResponder;
import client.responses.FunctionsResponder;
import client.responses.ProgramByNameResponder;
import display.*;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
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
import types.VarRefDTO;
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
    private Runnable onBackToOpening;
    private Consumer<String> onArchitectureSelected;

    private String userName;
    private ExecTarget targetKind;
    private String targetName;
    private int maxDegree;
    private int currentDegree = 0;
    private ExpandDTO lastExpanded = null;
    private String currentHighlight;
    private DisplayDTO display;

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

        // שינוי ארכיטקטורה — שלד (רק callback)
        if (cmbArchitecture != null) {
            cmbArchitecture.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (onArchitectureSelected != null) onArchitectureSelected.accept(nv);
            });
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
        }

        // 2) Summary follows the main program table
        if (summaryController != null && programTableController != null) {
            summaryController.wireTo(programTableController);
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
                display.ExpandDTO dto = (targetKind == utils.ExecTarget.PROGRAM)
                        ? client.responses.ExpandResponder.execute(target)
                        : client.responses.ExpandResponder.execute(targetName, target);
                Platform.runLater(() -> {
                    lastExpanded = dto;
                    if (programTableController != null) {
                        programTableController.showExpanded(dto);
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

    public void executeRun() {
        if (headerController == null || outputsController == null) return;
        List<Long> inputs = (inputsController != null)
                ? inputsController.collectValuesPadded()
                : List.of();

        final int degree = headerController.getCurrentDegree();
        final ExecutionRequestDTO dto = new ExecutionRequestDTO(degree, inputs);
        final String functionUserString =
                (targetKind == ExecTarget.FUNCTION) ? targetName : null;
        outputsController.clear();
        new Thread(() -> {
            try {
                Request req = Execute.build(dto, functionUserString);
                ExecutionDTO result = ExecuteResponder.execute(req);
                Platform.runLater(() -> {
                    outputsController.showExecution(result);
                    if (inputsController != null) {
                        inputsController.setInputsEditable(false);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                // אפשר להציג Alert קטן אם תרצי
            }
        }, "execute-run").start();
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

    public void setOnBackToOpening(Runnable cb) { this.onBackToOpening = cb; }
    public void setOnArchitectureSelected(Consumer<String> cb) { this.onArchitectureSelected = cb; }
    public void setDebugMode(boolean debug) {}
    public void setUserName(String name) {
        this.userName = name;
        if (headerController != null) headerController.setUserName(name);
    }
}
