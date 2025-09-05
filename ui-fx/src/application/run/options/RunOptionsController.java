package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class RunOptionsController {

    @FXML private Button btnStartRegular;
    @FXML private Button btnStartExecute;
    @FXML private Button btnStartDebug;
    @FXML private Button btnStop;
    @FXML private Button btnResume;
    @FXML private Button btnStepOver;
    //@FXML private Button btnStepBack;


    @FXML private void initialize() {
        // TODO: set initial disable/enable if needed
    }

    @FXML private void onStartRegular() { /* TODO */ }
    @FXML private void onStartExecute()     { /* TODO */ }
    @FXML private void onStartDebug()   { /* TODO */ }
    @FXML private void onStop()         { /* TODO */ }
    @FXML private void onResume()       { /* TODO */ }
    @FXML private void onStepOver()     { /* TODO */ }
    //@FXML private void onStepBack()     { /* TODO */ }

    // Optional helpers for later:
    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
