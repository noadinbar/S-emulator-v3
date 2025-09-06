package application; // שנה לפי החבילה שלך

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

// החליפי לחבילות שלך אם השמות שונים
import application.header.HeaderController;
import application.table.instruction.InstructionsController;
import application.summary.SummaryController;
import application.table.history.HistoryController;
import application.run.options.RunOptionsController;
import application.outputs.OutputsController;
import application.inputs.InputsController;
import display.Command2DTO;

// אם אצלך קיימים ה־API/DTO, תשאירי את ה־imports שלהם; אם לא – אפשר למחוק בשלב זה
import api.LoadAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
// import exportToDTO.LoadAPIImpl;
// import display.Command2DTO;
// import execution.ExecutionDTO;
// import execution.ExecutionRequestDTO;

import java.nio.file.Path;
import java.util.List;

public class ProgramSceneController {

    // ---- מכולות עיקריות (לא חובה, אבל נוח לגשת אליהן) ----
    @FXML private VBox leftColumn;   // fx:id="leftColumn" (לא חובה אם אין צורך)
    @FXML private VBox rightColumn;  // fx:id="rightColumn"

    // ---- תתי־קונטרולרים שמוזרקים דרך <fx:include> ----
    // שימי לב: השם הוא <fx:id> + "Controller"
    @FXML private HeaderController headerController;                 // fx:id="header"
    @FXML private InstructionsController programTableController;     // fx:id="programTable"
    @FXML private SummaryController summaryController;               // fx:id="summary"
    @FXML private InstructionsController chainTableController;       // fx:id="chainTable"
    @FXML private RunOptionsController runOptionsController;         // fx:id="runOptions"
    @FXML private OutputsController outputsController;               // fx:id="outputs"
    @FXML private InputsController inputsController;                 // fx:id="inputs"
    @FXML private HistoryController historyController;               // fx:id="history"

    // ---- אובייקטי מנוע (שלד; תחברי אותם כשתרצי) ----
    private LoadAPI loader;     // = new LoadAPIImpl();
    private DisplayAPI display; // יוגדר אחרי load
    private ExecutionAPI exec;  // יוגדר בזמן הרצה

    @FXML
    private void initialize() {


    }

    // ====== קרסים שתממשי מאוחר יותר (ריקים בכוונה) ======

    /** טעינת XML ועדכון כל המסכים. */
    private void handleLoadXmlRequested(Path xmlPath) {
        // TODO: loader.load → display; עדכון header/טבלאות/summary/inputs/history/outputs
    }

    /** הרצה רגילה/דיבאגר לפי הדגל. */
    private void handleRun(boolean debugMode) {
        // TODO: קריאת degree מה־Header, קבלת inputs, exec.execute/דיבאגר, עדכון outputs/history/chain
    }

    private void handleStop()   { /* TODO */ }
    private void handleResume() { /* TODO */ }
    private void handleStepOver(){ /* TODO */ }

    private void clearUI() {
        // TODO: לנקות טבלאות/סיכום/תוצאות/היסטוריה/כותרת
        // programTableController.clear(); chainTableController.clear(); ...
    }

    // --- עזר אופציונלי לממשק ציבורי אם תרצי לקרוא מחוץ לקונטרולר ---
    public void loadXml(Path xmlPath) throws Exception { /* TODO: לעטוף handleLoadXmlRequested */ }
    public void setInitialInputs(List<Long> inputs) { /* TODO */ }

    public void showCommand2(Command2DTO dto) {
        if (dto != null && programTableController != null) {
            programTableController.show(dto);
        }
        // >>> התוספת הנדרשת להצגת ה-Inputs מיד אחרי הטעינה <<<
        if (dto != null && inputsController != null) {
            inputsController.show(dto);
        }
        // (שאר הדברים יישארו כפי שהם עד שנממש אותם)
    }
}
