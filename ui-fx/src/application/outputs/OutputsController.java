package application.outputs;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;

public class OutputsController {
    // fx:id ב-FXML: txtVariables (TextArea), txtCycles (TextField)
    @FXML private TextArea txtVariables;
    @FXML private TextField txtCycles;

    @FXML private void initialize() {
        // TODO: txtVariables.setEditable(false); txtCycles.setEditable(false); etc.
    }

    /** ממלא משתנים מרשימת שורות "x1 = 5", "x2 = 9", ... */
    public void setVariableLines(List<String> lines) {
        // TODO
    }

    /** ממלא משתנים ממפה שם->ערך. */
    public void setVariables(Map<String, Long> vars) {
        // TODO
    }

    /** עדכון מספר הסייקלים. */
    public void setCycles(long cycles) {
        // TODO
    }

    /** ניקוי כל הפאנל. */
    public void clear() {
        // TODO
    }

    /** השבתה/הפעלה של הפאנל. */
    public void setDisabled(boolean disabled) {
        // TODO
    }
}
