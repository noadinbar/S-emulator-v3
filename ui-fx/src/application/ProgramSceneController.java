package application;

import javafx.fxml.FXML;
import javafx.application.Platform;


import application.header.HeaderController;
import application.table.instruction.InstructionsController;
import application.summary.SummaryController;
import application.table.history.HistoryController;
import application.run.options.RunOptionsController;
import application.outputs.OutputsController;
import application.inputs.InputsController;

import api.DisplayAPI;
import api.ExecutionAPI;

import display.Command2DTO;
import display.Command3DTO;
import display.InstructionDTO;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.HistoryDTO;
import execution.RunHistoryEntryDTO;

import types.VarOptionsDTO;
import types.VarRefDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgramSceneController {

    @FXML private HeaderController headerController;
    @FXML private InstructionsController programTableController;
    @FXML private SummaryController summaryController;
    @FXML private InstructionsController chainTableController;
    @FXML private RunOptionsController runOptionsController;
    @FXML private OutputsController outputsController;
    @FXML private InputsController inputsController;
    @FXML private HistoryController historyController;

    private DisplayAPI display;
    private ExecutionAPI execute;

    private int currentDegree = 0;

    @FXML
    private void initialize() {
        if (runOptionsController != null) {
            runOptionsController.setMainController(this);
        }

        if (headerController != null) {
            headerController.setOnLoaded(this::onProgramLoaded);
            headerController.setOnExpand(()  -> changeDegreeAndShow(+1));
            headerController.setOnCollapse(() -> changeDegreeAndShow(-1));
        }

        if (summaryController != null && programTableController != null) {
            summaryController.wireTo(programTableController);
        }

        if (programTableController != null && chainTableController != null) {
            wireAncestry();
        }

        if (programTableController != null) {
            programTableController.showLineColumn();
        }
        if (chainTableController != null) {
            chainTableController.hideLineColumn();
        }

    }


    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }

        if (display == null || inputsController == null) return;

        String csv = inputsController.collectValuesCsvPadded();
        if (outputsController != null) {
            outputsController.setVariableLines(List.of("Inputs: " + csv));
        }

        handleRun();
    }


    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.execute = display.execution();

        if (inputsController != null)  inputsController.clear();
        if (outputsController != null) outputsController.clear();
        if (historyController != null) historyController.clear();


        if (chainTableController != null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true);
        }

        int maxDegree = execute.getMaxDegree();
        currentDegree = 0;
        headerController.setMaxDegree(maxDegree);
        headerController.setCurrentDegree(currentDegree);

        if (programTableController != null) {
            Command3DTO expanded = this.display.expand(currentDegree);
            programTableController.showExpanded(expanded);
        }

        if (runOptionsController != null) {
            runOptionsController.setButtonsEnabled(true);
        }

        updateChain(programTableController != null ? programTableController.getSelectedItem() : null);
    }


    private void handleRun() {
        if (display == null) return;

        List<Long> inputs = inputsController.collectValuesPadded();
        int degree = headerController.getCurrentDegree();

        execute = display.executionForDegree(degree);
        ExecutionRequestDTO req = new ExecutionRequestDTO(degree, inputs);
        ExecutionDTO result = execute.execute(req);

        outputsController.showExecution(result);

        HistoryDTO hist = display.getHistory();
        List<RunHistoryEntryDTO> entries = (hist != null) ? hist.getEntries() : null;
        if (historyController != null && entries != null && !entries.isEmpty()) {
            RunHistoryEntryDTO last = entries.getLast();
            historyController.addEntry(last);
        }
    }

    private void changeDegreeAndShow(int i) {
        if (display == null || programTableController == null) return;

        int target = currentDegree + i;

        Command3DTO expanded = display.expand(target);
        programTableController.showExpanded(expanded);

        currentDegree = target;
        headerController.setCurrentDegree(currentDegree);


        updateChain(programTableController.getSelectedItem());
    }


    public void setDisplay(DisplayAPI display) {
        this.display = display;
        this.execute = null;
    }

    public void showCommand2(Command2DTO dto) {
        if (programTableController != null) {
            programTableController.show(dto);
            updateChain(null);
        }
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        Command2DTO dto = display.getCommand2();
        inputsController.show(dto);
        Platform.runLater(inputsController::focusFirstField);
    }


    private void wireAncestry() {
        updateChain(programTableController.getSelectedItem());
        programTableController.selectedItemProperty().addListener((obs, oldSel, newSel) -> updateChain(newSel));
    }


    private void updateChain(InstructionDTO selected) {
        if (chainTableController == null) return;

        if (selected == null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true);
            return;
        }


        List<InstructionDTO> ancestry = new ArrayList<>();
        ancestry.add(selected);
        ancestry.addAll(programTableController.getCreatedByChainFor(selected));

        Collections.reverse(ancestry);

        chainTableController.setRows(ancestry);
        chainTableController.getTableView().setDisable(ancestry.isEmpty());
    }


}
