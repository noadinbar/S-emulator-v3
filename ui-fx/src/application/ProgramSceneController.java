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

import display.Command2DTO;

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

    @FXML
    private void initialize() {
        if (runOptionsController != null) {
            runOptionsController.setMainController(this);
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
        handleRun(); // פנימי
    }

    /** לוגיקת הרצה בפועל (פרטי). */
    private void handleRun() {
        List<Long> inputs = inputsController.collectValuesPadded();

        if (exec == null) exec = display.execution();
        ExecutionRequestDTO req = new ExecutionRequestDTO(0, inputs); // degree=0 (AS IS)
        ExecutionDTO result = exec.execute(req);

        // --- הדפסות בדיקה לקונסול ---
        System.out.println("[RUN/CHECK] inputs (x1..xN) = " + inputs);
        System.out.println("[RUN/CHECK] y = " + result.getyValue());
        System.out.println("[RUN/CHECK] cycles = " + result.getTotalCycles());
        System.out.println("[RUN/CHECK] finals:");
        // --- סוף הדפסות ---

        Map<String, Long> vars = new LinkedHashMap<>();
        for (VarValueDTO vv : result.getFinals()) {
            String name = formatVarName(vv.getVar());
            Long value = vv.getValue();

            // הדפסת כל משתנה ותוצאתו
            System.out.println("    " + name + " = " + value);

            vars.put(name, value);
        }

        outputsController.setVariables(vars);
        outputsController.setCycles(result.getTotalCycles());
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
        // אם יש: inputsController.focusFirstField();
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
