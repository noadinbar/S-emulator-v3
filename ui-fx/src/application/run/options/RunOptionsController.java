package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import application.ProgramSceneController;

public class RunOptionsController {

    @FXML private Button btnStartRegular;
    @FXML private Button btnStartExecute; // "Start" — יציג את ה-Inputs לעריכה
    @FXML private Button btnStartDebug;
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
        // TODO: set initial disable/enable if needed
    }

    @FXML private void onStartRegularAction() { main.showInputsForEditing(); }

    // START: מציג את רשימת ה-Inputs ומאפשר עריכה (בלי להריץ)
    @FXML private void onStartExecuteAction() { main.showInputsForEditing(); } // <-- היה main.runExecute();

    @FXML private void onStartDebugAction()   { /* TODO */ }
    @FXML private void onStopAction()         { /* TODO */ }
    @FXML private void onResumeAction()       { /* TODO */ }
    @FXML private void onStepOverAction()     { /* TODO */ }
    //@FXML private void onStepBack()   { /* TODO */ }

    // Optional helpers for later:
    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
