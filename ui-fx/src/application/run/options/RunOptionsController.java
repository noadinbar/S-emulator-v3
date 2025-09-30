package application.run.options;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import application.ProgramSceneController;
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

    private ProgramSceneController main;
    private final BooleanProperty forceExecuteEnabled = new SimpleBooleanProperty(false);

    public void setMainController(ProgramSceneController main) {
        this.main = main;
    }

    @FXML
    private void initialize() {
        // מצב התחלתי: Start כבוי, כפתורי דיבאג כבויים
        startEnabled(false);
        setButtonsEnabled(false);

        // טוגל Run: מבטל Debug ומכבה כפתורי דיבאג
        chkRun.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkDebug.setSelected(false);
                if (main != null) main.setDebugMode(false);
                setDebugBtnsDisabled(true);
            }
        });

        // טוגל Debug: מדליק/מכבה מצב דיבאג ואת כפתורי הדיבאג
        chkDebug.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                chkRun.setSelected(false);
                if (main != null) main.setDebugMode(true);
                // לא מדליקים כאן את כפתורי הדיבאג!
                setDebugBtnsDisabled(true);
            } else {
                if (main != null) main.setDebugMode(false);
                setDebugBtnsDisabled(true);
            }
        });


        // Execute זמין רק כשמסומן Run או Debug (או כשעושים override דרך forceExecuteEnabled)
        btnExecute.disableProperty().bind(
                chkRun.selectedProperty().or(chkDebug.selectedProperty()).or(forceExecuteEnabled).not()
        );

        // לכבות Start כל עוד מסומן Debug
        chkDebug.selectedProperty().addListener((o, was, is) -> {
            if (btnStart != null) btnStart.setDisable(is);
        });
// סנכרון התחלתי (למקרה שה־FXML מסמן Debug כברירת מחדל)
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

        // חשוב: לא להדליק כאן את כפתורי הדיבאג כשהמסך "נפתח".
        // הם יידלקו כאשר chkDebug יסומן, או ש־ProgramSceneController יקרא setDebugBtnsDisabled(false)
        // כשנכנסים לדיבאג (למשל אחרי ריצה "שקטה" עד ה-BP).
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

    public void setDebugSelected(boolean selected) {
        if (chkDebug != null) chkDebug.setSelected(selected);
    }

    /**
     * היסטורית שימש לנעילת בחירה כשיש BP. ב-flow הנוכחי אין שימוש – השארתי לטובת תאימות.
     */
    public void setDebugSelectorLocked(boolean locked) {
        if (chkRun  != null) chkRun.setDisable(locked);
        if (chkDebug!= null) chkDebug.setDisable(locked);
    }

    public void setForceExecuteEnabled(boolean enabled) {
        forceExecuteEnabled.set(enabled);
    }

    @FXML private void onStartAction() {
        main.showInputsForEditing();
        setButtonsEnabled(true);
        chkRun.setDisable(false);
        chkDebug.setDisable(false);
    }

    @FXML private void onRunAction()   { main.runExecute(); }
    @FXML private void onDebugAction() { main.runExecute(); }
    @FXML private void onStopAction()  { main.debugStop(); }

    @FXML private void onResumeAction() {
        chkDebug.setSelected(true);
        main.debugResume();
    }

    @FXML private void onStepOverAction() {
        chkDebug.setSelected(true);
        main.debugStep();
    }

    @FXML
    private void onStepBackAction() {
        chkDebug.setSelected(true);
        main.debugStepBack();
    }

    @FXML
    private void onExecuteAction() {
        if (chkRun.isSelected()) {
            onRunAction();
            startEnabled(true);
            setButtonsEnabled(false);
        } else if (chkDebug.isSelected()) {
            onDebugAction();
        } else {
            main.runExecute();
        }
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