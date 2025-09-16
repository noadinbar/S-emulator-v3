package application.run.options;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import application.ProgramSceneController;
import javafx.scene.control.CheckBox;


public class RunOptionsController {

    @FXML private Button btnStart;
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

        // כשמסמנים Run – מכבים Debug ומודיעים ל-Main
        chkRun.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkDebug.setSelected(false);
                if (main != null) main.setDebugMode(false);
                btnStepOver.setDisable(true);
                btnResume.setDisable(true);
                btnStop.setDisable(true);
            }
        });

        chkDebug.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkRun.setSelected(false);
                if (main != null) main.setDebugMode(true);
                // במצב Debug: כפתורי דיבאג דולקים
                btnStepOver.setDisable(false);
                btnResume.setDisable(false);
                btnStop.setDisable(false);
            } else {
                if (main != null) main.setDebugMode(false);
                btnStepOver.setDisable(true);
                btnResume.setDisable(true);
                btnStop.setDisable(true);
            }
        });

        btnExecute.disableProperty().bind(
                chkRun.selectedProperty().or(chkDebug.selectedProperty()).not()
        );
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

    @FXML private void onDebugAction() { // Execute כש-Debug מסומן
        if (main != null) main.runExecute(); // ב-ProgramSceneController זה יבצע debugStep()
    }

    @FXML private void onStopAction()  {
        if (main != null) main.debugStop();
    }

    @FXML private void onResumeAction() {
        if (main != null) {
            chkDebug.setSelected(true); // מוודא ש-Debug פעיל
            main.debugResume();
        }
    }

    @FXML private void onStepOverAction() {
        if (main != null) {
            chkDebug.setSelected(true);
            main.debugStep();
        }
    }

    //@FXML private void onStepBack()   { /* TODO */ }

    @FXML
    private void onExecuteAction() {
        if (chkRun.isSelected()) {
            onRunAction();
        } else if ( chkDebug.isSelected()) {
            onDebugAction();        // עתידי; כרגע יש לך TODO
        }
    }

    public void setResumeBusy(boolean busy) {
        if (busy) {
            // בזמן Resume: משביתים הכול חוץ מ-Stop
            if (btnStop != null)     btnStop.setDisable(false);
            if (btnResume != null)   btnResume.setDisable(true);
            if (btnStepOver != null) btnStepOver.setDisable(true);
            if (btnExecute != null)  btnExecute.setDisable(true);
            if (chkRun != null)      chkRun.setDisable(true);
            if (chkDebug != null)    chkDebug.setDisable(true);
        } else {
            // חזרה למצב רגיל לפי מה שמסומן
            boolean debugOn = chkDebug != null && chkDebug.isSelected();
            boolean anyMode = (chkRun != null && chkRun.isSelected()) || debugOn;

            if (btnStop != null)     btnStop.setDisable(!debugOn);
            if (btnResume != null)   btnResume.setDisable(!debugOn);
            if (btnStepOver != null) btnStepOver.setDisable(!debugOn);
            if (btnExecute != null)  btnExecute.setDisable(!anyMode);
            if (chkRun != null)      chkRun.setDisable(false);
            if (chkDebug != null)    chkDebug.setDisable(false);
        }
    }

    public void setRunning(boolean running) { /* TODO */ }
    public void setDebugMode(boolean debug) { /* TODO */ }
}
