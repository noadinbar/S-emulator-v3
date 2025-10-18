package application.execution.run.options;

import application.execution.ExecutionSceneController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

public class RunOptionsController {

    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;
    @FXML private Button btnStepBack;
    @FXML private CheckBox chkRun;
    @FXML private CheckBox chkDebug;
    @FXML private Button btnExecute;

    private ExecutionSceneController main;
    private final BooleanProperty forceExecuteEnabled = new SimpleBooleanProperty(false);

    public void setMainController(ExecutionSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {
        startEnabled(false);
        setButtonsEnabled(false);
        chkRun.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkDebug.setSelected(false);
                if (main != null) main.setDebugMode(false);
                setDebugBtnsDisabled(true);
            }
        });

        chkDebug.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkRun.setSelected(false);
                if (main != null) main.setDebugMode(true);
                setDebugBtnsDisabled(true);
            } else {
                if (main != null) main.setDebugMode(false);
                setDebugBtnsDisabled(true);
            }
        });
        btnExecute.disableProperty().bind(
                chkRun.selectedProperty().or(chkDebug.selectedProperty()).or(forceExecuteEnabled).not()
        );
        chkDebug.selectedProperty().addListener((o, was, is) -> {
            if (btnStart != null) btnStart.setDisable(is);
        });
        if (btnStart != null) btnStart.setDisable(chkDebug.isSelected());

    }

    public void startEnabled(boolean enabled) {
        if (btnStart != null) btnStart.setDisable(!enabled);
    }


    public void setButtonsEnabled(boolean enabled) {
        boolean disable = !enabled;

        if (chkRun != null) {
            chkRun.setDisable(disable);
            if (disable) chkRun.setSelected(false);
        }
        if (chkDebug != null) {
            chkDebug.setDisable(disable);
            if (disable) chkDebug.setSelected(false);
        }
        setDebugBtnsDisabled(true);
    }

    public void clearRunCheckBox() {
        if (chkRun  != null) chkRun.setSelected(false);
        if (chkDebug!= null) chkDebug.setSelected(false);
        setDebugBtnsDisabled(true);
    }

    public void setDebugBtnsDisabled(boolean disabled) {
        if (btnStepOver != null) btnStepOver.setDisable(disabled);
        if (btnResume != null) btnResume.setDisable(disabled);
        if (btnStop != null) btnStop.setDisable(disabled);
        if (btnStepBack != null) btnStepBack.setDisable(disabled);
    }

    public void setStepBackDisabled(boolean disabled) {
        if (btnStepBack != null) btnStepBack.setDisable(disabled);
    }

    // --- Actions (wired from run_options.fxml) ---

    @FXML private void onStartAction() {
        // TODO: wire start later (open inputs, etc.)
        startEnabled(true);
        setButtonsEnabled(true);
    }

    @FXML private void onStopAction() {
        // TODO: implement stop later
        startEnabled(false);
        setButtonsEnabled(false);
    }

    @FXML private void onResumeAction() {
        // TODO: implement resume later
        chkDebug.setSelected(true);
    }

    @FXML private void onStepOverAction() {
        // TODO: implement step-over later
        chkDebug.setSelected(true);
    }

    @FXML private void onStepBackAction() {
        // TODO: implement step-back later
        chkDebug.setSelected(true);
    }

    @FXML private void onExecuteAction() {
        // For now: just simulate start; later we'll actually execute DisplayDTO
        boolean run = chkRun != null && chkRun.isSelected();
        boolean debug = chkDebug != null && chkDebug.isSelected();

        startEnabled(true);
        setButtonsEnabled(debug); // אם Debug מסומן, השאירי כפתורי דיבוג פעילים
    }


    public void setResumeBusy(boolean busy) {
        if (busy) {
            if (btnStop != null)     btnStop.setDisable(false);
            if (btnResume != null)   btnResume.setDisable(true);
            if (btnStepOver != null) btnStepOver.setDisable(true);
            if (chkRun != null)      chkRun.setDisable(true);
            if (chkDebug != null)    chkDebug.setDisable(true);
        }
        else {
            boolean debugOn = chkDebug != null && chkDebug.isSelected();
            if (btnStop != null)     btnStop.setDisable(!debugOn);
            if (btnResume != null)   btnResume.setDisable(!debugOn);
            if (btnStepOver != null) btnStepOver.setDisable(!debugOn);
            if (chkRun != null)      chkRun.setDisable(false);
            if (chkDebug != null)    chkDebug.setDisable(false);
        }
    }
}