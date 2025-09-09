package application;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

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
    @FXML private InstructionsController programTableController; // טבלה עליונה (Program)
    @FXML private SummaryController summaryController;
    @FXML private InstructionsController chainTableController;   // טבלה תחתונה (Chain)
    @FXML private RunOptionsController runOptionsController;
    @FXML private OutputsController outputsController;
    @FXML private InputsController inputsController;
    @FXML private HistoryController historyController;

    private DisplayAPI display; // מוזרק אחרי LOAD
    private ExecutionAPI exec;  // נוצר בזמן הרצה

    private int currentDegree = 0;
    private int maxDegree = 0;

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
            wireLineage();
        }

        if (programTableController != null) {
            programTableController.showLineColumn();   // הטבלה העליונה: להציג Line
        }
        if (chainTableController != null) {
            chainTableController.hideLineColumn();     // הטבלה התחתונה: להסתיר Line
        }

    }

    /** נקראת מכפתור "Start Execute" — מבצעת בדיקות ואז קוראת פנימית ל-handleRun(). */
    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }

        if (display == null || inputsController == null) return;

        String csv = inputsController.collectValuesCsvPadded();
        if (outputsController != null) {
            outputsController.setVariableLines(java.util.List.of("Inputs: " + csv));
        }

        handleRun();
    }

    // נקראת ע"י HeaderController אחרי טעינת XML מוצלחת
    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.exec = display.execution();

        if (inputsController != null)  inputsController.clear();
        if (outputsController != null) outputsController.clear();
        if (historyController != null) historyController.clear();

        // איפוס וסטטוס שרשראות
        if (chainTableController != null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true);
        }

        maxDegree = exec.getMaxDegree();
        currentDegree = 0;
        headerController.setMaxDegree(maxDegree);
        headerController.setCurrentDegree(currentDegree);

        // מציגים דרגה 0 בטבלה העליונה (כולל ExpandedInstructionDTO mapping)
        if (programTableController != null) {
            Command3DTO c3 = this.display.expand(currentDegree);
            programTableController.showExpanded(c3);
        }

        if (runOptionsController != null) {
            runOptionsController.setButtonsEnabled(true);
        }

        // עדכון chain ראשוני (אם יש בחירה)
        updateChain(programTableController != null ? programTableController.getSelectedItem() : null);
    }

    /** לוגיקת הרצה בפועל. */
    private void handleRun() {
        if (display == null) return;

        List<Long> inputs = inputsController.collectValuesPadded();
        int degree = headerController.getCurrentDegree();

        exec = display.executionForDegree(degree);
        ExecutionRequestDTO req = new ExecutionRequestDTO(degree, inputs);
        ExecutionDTO result = exec.execute(req);

        outputsController.showExecution(result);

        // היסטוריה: מוסיפים את הריצה האחרונה אם קיימת
        HistoryDTO hist = display.getHistory();
        List<RunHistoryEntryDTO> entries = (hist != null) ? hist.getEntries() : null;
        if (historyController != null && entries != null && !entries.isEmpty()) {
            RunHistoryEntryDTO last = entries.get(entries.size() - 1);
            historyController.addEntry(last);
        }
    }

    private void changeDegreeAndShow(int delta) {
        if (display == null || programTableController == null) return;

        int target = currentDegree + delta;

        Command3DTO c3 = display.expand(target);
        programTableController.showExpanded(c3); // מציג אחרי הרחבה

        currentDegree = target;
        headerController.setCurrentDegree(currentDegree);

        // עדכון טבלת השרשרת בהתאם לבחירה הנוכחית
        updateChain(programTableController.getSelectedItem());
    }

    /** מוזנקת לאחר LOAD; מאפסת גם מופע Execution. */
    public void setDisplay(DisplayAPI display) {
        this.display = display;
        this.exec = null;
    }

    public void showCommand2(Command2DTO dto) {
        if (programTableController != null) {
            programTableController.show(dto);
            // אם מראים C2 רגיל — אין שרשרת; מרוקנים את הטבלה התחתונה
            updateChain(null);
        }
    }

    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        Command2DTO dto = display.getCommand2();
        inputsController.show(dto);
        Platform.runLater(inputsController::focusFirstField);
    }

    /** חיבור הבחירה בטבלה העליונה לעדכון טבלת השרשרת. */
    private void wireLineage() {
        updateChain(programTableController.getSelectedItem()); // ראשוני
        programTableController.selectedItemProperty().addListener((obs, oldSel, newSel) -> updateChain(newSel));
    }

    /** בונה רשימה: [נוכחית, אב מיידי, ..., מקורית] ומציג בטבלה התחתונה. */
    private void updateChain(InstructionDTO selected) {
        if (chainTableController == null) return;

        if (selected == null) {
            chainTableController.clear();
            chainTableController.getTableView().setDisable(true); // אופציונלי
            return;
        }

        // בונים את הרשימה כמו קודם: [נוכחית, אב מיידי, ..., מקורית]
        List<InstructionDTO> lineage = new ArrayList<>();
        lineage.add(selected);
        lineage.addAll(programTableController.getCreatedByChainFor(selected));

        // שינוי כאן: מציגים הפוך — מהסוף להתחלה (מקורית למעלה, נוכחית בסוף)
        Collections.reverse(lineage);

        chainTableController.setRows(lineage);
        chainTableController.getTableView().setDisable(lineage.isEmpty()); // אופציונלי
    }


    // --- עזרים --- (נשארים אם צריך במקום אחר)
    private static String formatVarName(VarRefDTO v) {
        if (v == null) return "";
        if (v.getVariable() == VarOptionsDTO.y) return "y";
        String base = (v.getVariable() == VarOptionsDTO.x) ? "x" : "z";
        return base + v.getIndex();
    }
}
