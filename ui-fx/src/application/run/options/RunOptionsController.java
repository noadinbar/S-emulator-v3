package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import application.ProgramSceneController;
import javafx.scene.control.CheckBox;


public class RunOptionsController {

    @FXML private Button btnStart;
    @FXML private Button btnRun;
    @FXML private Button btnDebug;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;
    @FXML private CheckBox chkRun;
    @FXML private CheckBox chkDebug;
    @FXML private Button btnExecute;

    private ProgramSceneController main;

    public void setMainController(ProgramSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {
        startEnabled(false);
        setButtonsEnabled(false);
        chkRun.selectedProperty().addListener((o, was, is) -> { if (is) chkDebug.setSelected(false); });
        chkDebug.selectedProperty().addListener((o, was, is) -> { if (is) chkRun.setSelected(false); });
        btnExecute.disableProperty().bind(chkRun.selectedProperty().or(chkDebug.selectedProperty()).not());

    }

    public void startEnabled(boolean enabled) {if (btnStart != null) btnStart.setDisable(!enabled);}

    public void setButtonsEnabled(boolean enabled) {
        boolean disable = !enabled;
        if (btnStop != null)         btnStop.setDisable(disable);
        if (btnResume != null)       btnResume.setDisable(disable);
        if (btnStepOver != null)     btnStepOver.setDisable(disable);
        if (chkRun != null) chkRun.setDisable(disable);
        if (chkDebug != null)  chkDebug.setDisable(disable);
    }


    @FXML private void onStartAction() {
        main.showInputsForEditing();
        setButtonsEnabled(true);
        chkRun.setDisable(false);
        chkDebug.setDisable(false);
    }


    @FXML private void onRunAction() { main.runExecute(); }
    @FXML private void onDebugAction()   { /* TODO */ }
    @FXML private void onStopAction()         { /* TODO */ }
    @FXML private void onResumeAction()       { /* TODO */ }
    @FXML private void onStepOverAction()     { /* TODO */ }
    //@FXML private void onStepBack()   { /* TODO */ }

    @FXML
    private void onExecuteAction() {
        if (chkRun.isSelected()) {
            onRunAction();
        } else if ( chkDebug.isSelected()) {
            onDebugAction();        // עתידי; כרגע יש לך TODO
        }
    }
    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
