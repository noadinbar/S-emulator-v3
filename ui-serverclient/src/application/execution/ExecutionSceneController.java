package application.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import application.opening.OpeningSceneController;
import client.responses.ExpandResponder;
import client.responses.FunctionsResponder;
import client.responses.ProgramByNameResponder;
import display.*;
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
        }

        // 2) Summary follows the main program table
        if (summaryController != null && programTableController != null) {
            summaryController.wireTo(programTableController);
        }

        // 3) Load DisplayDTO off the FX thread
        new Thread(() -> {
            try {
                DisplayDTO dto = (target == ExecTarget.PROGRAM)
                        ? ProgramByNameResponder.execute(name)    // by program name
                        : FunctionsResponder.program(name);      // by function user-string (key)

                if (dto == null) return;
                Platform.runLater(() -> {
                    // degree 0 flat instructions into the main table
                    programTableController.show(dto.getInstructions());
                    // chain table stays empty for now (we'll use it on expand/debug)
                    if (chainTableController != null) {
                        chainTableController.show(List.of());
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
        System.out.println("[EXEC] A doApply: requested=" + requestedDegree + " max=" + maxDegree);

        final int target = Math.max(0, Math.min(requestedDegree, maxDegree));
        currentDegree = target;
        if (headerController != null) headerController.setCurrentDegree(currentDegree);

        // ---- דיבוג סביב Thread ----
        Runnable job = () -> {
            System.out.println("[EXEC] D THREAD enter (deg=" + target + ")");
            try {
                display.ExpandDTO dto = (targetKind == utils.ExecTarget.PROGRAM)
                        ? client.responses.ExpandResponder.execute(target)
                        : client.responses.ExpandResponder.execute(targetName, target);
                System.out.println("[EXEC] D1 THREAD got response: " + (dto != null));
                javafx.application.Platform.runLater(() -> {
                    System.out.println("[EXEC] D2 FX apply dto");
                    lastExpanded = dto;
                    if (dto != null && dto.getMaxDegree() != maxDegree) {
                        maxDegree = dto.getMaxDegree();
                        if (headerController != null) headerController.setMaxDegree(maxDegree);
                    }
                });
            } catch (Exception ex) {
                System.out.println("[EXEC] D! THREAD ERROR: " + ex);
                ex.printStackTrace();
            }
        };

        try {
            System.out.println("[EXEC] B about to create thread");
            Thread t = new Thread(job, "expand-" + target);
            System.out.println("[EXEC] B1 created: " + t);
            t.setDaemon(true);
            System.out.println("[EXEC] B2 starting...");
            t.start();
            System.out.println("[EXEC] C started, isAlive=" + t.isAlive());
        } catch (Throwable th) {
            System.out.println("[EXEC] B! failed to start thread: " + th);
            th.printStackTrace();
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
