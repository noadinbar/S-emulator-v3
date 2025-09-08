package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import application.ProgramSceneController;

public class RunOptionsController {

    @FXML private Button btnStart;
    @FXML private Button btnRun; // "Start" — יציג את ה-Inputs לעריכה
    @FXML private Button btnDebug;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;
    //@FXML private Button btnStepBack;

    // רפרנס לקונטרולר הראשי מוזרק מה-ProgramSceneController.initialize()
    private ProgramSceneController main;

    public void setMainController(ProgramSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {
        setButtonsEnabled(false);
    }

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

    // START: מציג את רשימת ה-Inputs ומאפשר עריכה (בלי להריץ)
    @FXML private void onRunAction() { main.runExecute(); }

    @FXML private void onDebugAction()   { /* TODO */ }
    @FXML private void onStopAction()         { /* TODO */ }
    @FXML private void onResumeAction()       { /* TODO */ }
    @FXML private void onStepOverAction()     { /* TODO */ }
    //@FXML private void onStepBack()   { /* TODO */ }

    // Optional helpers for later:
    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
