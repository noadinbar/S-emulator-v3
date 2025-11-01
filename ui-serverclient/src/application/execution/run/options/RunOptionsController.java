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
    @FXML private Button btnExecute;
    @FXML private CheckBox chkRun;
    @FXML private CheckBox chkDebug;

    private ExecutionSceneController main;

    // Execute button is disabled when this is true.
    private final BooleanProperty executeForceDisabled = new SimpleBooleanProperty(false);

    public void setMainController(ExecutionSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {
        startEnabled(false);
        setButtonsEnabled(false);      // disables both RUN/DEBUG choices initially
        setDebugBtnsDisabled(true);    // and all debug controls

        // Toggle RUN/DEBUG (mutually exclusive)
        if (chkRun != null && chkDebug != null) {
            chkRun.selectedProperty().addListener((o, was, is) -> {
                if (is) {
                    chkDebug.setSelected(false);
                    if (main != null) main.setDebugMode(false);
                    setDebugBtnsDisabled(true);   // keep debug buttons OFF
                    if (btnStart != null) btnStart.setDisable(false);
                    setExecuteForceDisabled(false);
                }
            });
            chkDebug.selectedProperty().addListener((o, was, is) -> {
                if (is) {
                    chkRun.setSelected(false);
                    if (main != null) main.setDebugMode(true);
                    // IMPORTANT: do NOT enable debug buttons here.
                    // They will be enabled AFTER successful debug init from the main controller.
                    setDebugBtnsDisabled(true);
                    if (btnStart != null) btnStart.setDisable(true);
                    setExecuteForceDisabled(false);
                } else {
                    setDebugBtnsDisabled(true);
                    if (main != null) main.setDebugMode(false);
                    if (btnStart != null) btnStart.setDisable(false);
                }
            });
        }

        // Enable Execute only when a mode is selected and not force-disabled
        if (btnExecute != null && chkRun != null && chkDebug != null) {
            btnExecute.disableProperty().bind(
                    executeForceDisabled.or(
                            chkRun.selectedProperty().or(chkDebug.selectedProperty()).not()
                    )
            );
        }
    }

    @FXML
    private void onStartAction() {
        // “Start” = open inputs for editing
        setButtonsEnabled(true);
        if (chkRun   != null) chkRun.setDisable(false);
        if (chkDebug != null) chkDebug.setDisable(false);
        if (main != null) main.showInputsForEditing();
    }
    @FXML
    private void onExecuteAction() {
        setExecuteForceDisabled(true);
        if (main != null) main.runExecute();
        main.lockAfterExecute();
    }

    @FXML
    private void onResumeAction() {
        if (main != null) main.debugResume();
    }

    @FXML
    private void onStepOverAction() {
        if (main != null) main.debugStep();
    }

    @FXML
    private void onStopAction() {
        if (main != null) main.debugStop();
        startEnabled(false);
        setButtonsEnabled(false);
        setDebugBtnsDisabled(true);
    }

    @FXML
    private void onStepBackAction() {
        //if (main != null) main.debugStepBack(); // v2 name (no-op for now)
        if (chkDebug != null) chkDebug.setSelected(true);
    }


    /** Enable/disable only the debug control buttons (Resume/Step/Stop/StepBack). */
    public void setDebugBtnsDisabled(boolean disabled) {
        if (btnStepOver != null) btnStepOver.setDisable(disabled);
        if (btnResume   != null) btnResume.setDisable(disabled);
        if (btnStop     != null) btnStop.setDisable(disabled);
    }

    /** Enable/disable the RUN/DEBUG selectors as a group; keeps debug buttons OFF. */
    public void setButtonsEnabled(boolean enabled) {
        final boolean disable = !enabled;
        if (chkRun   != null) { chkRun.setDisable(disable);   if (disable) chkRun.setSelected(false);   }
        if (chkDebug != null) { chkDebug.setDisable(disable); if (disable) chkDebug.setSelected(false); }
        setDebugBtnsDisabled(true);
    }

    public void clearRunCheckBox() {
        if (chkRun   != null) chkRun.setSelected(false);
        if (chkDebug != null) chkDebug.setSelected(false);
        setDebugBtnsDisabled(true);
    }

    /** Controls the START button only. */
    public void startEnabled(boolean enabled) {
        if (btnStart != null) btnStart.setDisable(!enabled);
    }

    // Optional external control if you ever need to lock the Execute button
    public void setExecuteForceDisabled(boolean disabled) {
        executeForceDisabled.set(disabled);
    }

    public void applyPreset(boolean debugSelected) {
        // allow user to interact with the run/debug mode right away
        setButtonsEnabled(true);
        // allow Start button to be clickable (UI feels "armed")
        startEnabled(true);
        if (debugSelected) {
            // choose DEBUG mode in the UI
            if (chkDebug != null) {
                chkDebug.setSelected(true);
            }
            if (chkRun != null) {
                chkRun.setSelected(false);
            }
        } else {
            // choose RUN mode in the UI
            if (chkRun != null) {
                chkRun.setSelected(true);
            }
            if (chkDebug != null) {
                chkDebug.setSelected(false);
            }
        }
    }

}
