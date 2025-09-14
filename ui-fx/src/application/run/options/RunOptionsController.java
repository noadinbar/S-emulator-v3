package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import application.ProgramSceneController;


public class RunOptionsController {

    @FXML private Button btnStart;
    @FXML private Button btnRun;
    @FXML private Button btnDebug;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;



    private ProgramSceneController main;

    public void setMainController(ProgramSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {  setButtonsEnabled(false);}

    public void setButtonsEnabled(boolean enabled) {
        boolean disable = !enabled;
        if (btnStart != null) btnStart.setDisable(disable);
        if (btnRun != null) btnRun.setDisable(disable);
        if (btnDebug != null)   btnDebug.setDisable(disable);
        if (btnStop != null)         btnStop.setDisable(disable);
        if (btnResume != null)       btnResume.setDisable(disable);
        if (btnStepOver != null)     btnStepOver.setDisable(disable);
    }


    @FXML private void onStartAction() { main.showInputsForEditing(); }


    @FXML private void onRunAction() { main.runExecute(); }
    @FXML private void onDebugAction()   { /* TODO */ }
    @FXML private void onStopAction()         { /* TODO */ }
    @FXML private void onResumeAction()       { /* TODO */ }
    @FXML private void onStepOverAction()     { /* TODO */ }
    //@FXML private void onStepBack()   { /* TODO */ }
    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
