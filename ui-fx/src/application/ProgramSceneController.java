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
import execution.HistoryDTO;
import execution.RunHistoryEntryDTO;

import display.Command2DTO;
import display.Command3DTO;
import display.ExpandedInstructionDTO;
import display.InstructionDTO;
import java.util.stream.Collectors;

import api.DisplayAPI;
import api.ExecutionAPI;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;

import types.VarOptionsDTO;
import types.VarRefDTO;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProgramSceneController {

    @FXML private VBox leftColumn;
    @FXML private VBox rightColumn;

    @FXML private HeaderController headerController;
    @FXML private InstructionsController programTableController;
    @FXML private SummaryController summaryController;
    @FXML private InstructionsController chainTableController;
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

    }

    /** נקראת מכפתור "Start Execute" — מבצעת בדיקות ואז קוראת פנימית ל-handleRun(). */
    public void runExecute() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::runExecute);
            return;
        }

        if (display == null || inputsController == null ) {
            return; // אפשר להחליף בהודעת שגיאה ייעודית אם תרצי
        }
        String csv = inputsController.collectValuesCsvPadded();

        // 2) מציגים את ה-CSV למעלה באיזור ה-variables (כקו ראשון), לפני ההרצה
        if (outputsController != null) {
            outputsController.setVariableLines(java.util.List.of("Inputs: " + csv));
        }

        handleRun(); // פנימי
    }


    // חדש: נקרא ע"י HeaderController אחרי טעינת XML מוצלחת
    private void onProgramLoaded(DisplayAPI display) {
        this.display = display;
        this.exec = display.execution();

        if (inputsController != null)  inputsController.clear();   // <<< חדש
        if (outputsController != null) outputsController.clear();  // <<< חדש
        if (historyController != null) historyController.clear();

        maxDegree = exec.getMaxDegree();
        currentDegree = 0;
        headerController.setMaxDegree(maxDegree);
        headerController.setCurrentDegree(currentDegree);

        // מציגים את התוכנית המורחבת לדרגה 0 בטבלת ה-Instructions
        if (programTableController != null) {
            Command3DTO c3 = this.display.expand(currentDegree);
            programTableController.showExpanded(c3);
        }
        if (runOptionsController != null)   runOptionsController.setButtonsEnabled(true);

    }


    /** לוגיקת הרצה בפועל (פרטי). */
    private void handleRun() {
        List<Long> inputs = inputsController.collectValuesPadded();

        if (exec == null) exec = display.execution();
        ExecutionRequestDTO req = new ExecutionRequestDTO(0, inputs); // degree=0 (AS IS)
        ExecutionDTO result = exec.execute(req);



        Map<String, Long> vars = new LinkedHashMap<>();
        for (VarValueDTO vv : result.getFinals()) {
            String name = formatVarName(vv.getVar());
            Long value = vv.getValue();
            vars.put(name, value);
        }

        outputsController.setVariables(vars);
        outputsController.setCycles(result.getTotalCycles());
        // --- היסטוריה מתוך ה-DTO של המנוע ---
        execution.HistoryDTO hist = display.getHistory();
        List<execution.RunHistoryEntryDTO> entries =
                (hist != null) ? hist.getEntries() : null;

        if (historyController != null && entries != null && !entries.isEmpty()) {
            RunHistoryEntryDTO last = entries.get(entries.size() - 1);
            historyController.addEntry(last);


        }

    }

    private void changeDegreeAndShow(int delta) {
        if (display == null || programTableController == null) return;

        int target = currentDegree + delta;


        // כאן מתרחשת הקריאה ל-expand של המנוע!
        Command3DTO c3 = display.expand(target);
        programTableController.showExpanded(c3); // מציג את שורות ה-Instruction אחרי ההרחבה

        currentDegree = target;
        headerController.setCurrentDegree(currentDegree); // מעדכן תצוגת "current / max"
    }

    /** מוזנקת לאחר LOAD; מאפסת גם מופע Execution. */
    public void setDisplay(DisplayAPI display) {
        this.display = display;
        this.exec = null;
    }

    public void showCommand2(Command2DTO dto) {
        if (programTableController != null) {
            programTableController.show(dto);
        }
    }

    /** מוזנק מכפתור START כדי לפתוח/לערוך אינפוטים. */
    public void showInputsForEditing() {
        if (display == null || inputsController == null) return;
        Command2DTO dto = display.getCommand2();
        inputsController.show(dto);
       Platform.runLater(inputsController::focusFirstField);

    }

    // --- עזרים ---
    private static String formatVarName(VarRefDTO v) {
        if (v == null) return "";
        if (v.getVariable() == VarOptionsDTO.y) return "y";
        String base = (v.getVariable() == VarOptionsDTO.x) ? "x" : "z";
        return base + v.getIndex();
    }

    // (שאר הקרסים/ניקוי UI/handleLoadXmlRequested נשארים כמו אצלך או TODO)


}
