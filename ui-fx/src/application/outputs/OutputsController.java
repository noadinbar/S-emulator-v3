package application.outputs;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OutputsController {
    // fx:id: ב-FXML – txtVariables (TextArea), txtCycles (TextField)
    @FXML private TextArea txtVariables;
    @FXML private TextField txtCycles;

    @FXML
    private void initialize() {
        if (txtVariables != null) {
            txtVariables.setEditable(false);
            txtVariables.setWrapText(true);
        }
        if (txtCycles != null) {
            txtCycles.setEditable(false);
        }
    }

    /** ממלא משתנים מרשימת שורות "x1 = 5", "x2 = 9", ... */
    public void setVariableLines(List<String> lines) {
        if (txtVariables == null) return;
        String text = (lines == null) ? "" : String.join("\n", lines);
        txtVariables.setText(text);
    }

    /** ממלא משתנים ממפה שם->ערך. */
    public void setVariables(Map<String, Long> vars) {
        if (txtVariables == null || vars == null) {
            if (txtVariables != null) txtVariables.clear();
            return;
        }
        // שורות בפורמט name = value (ממויין לפי שם, כדי שיפה לעין)
        String text = vars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n"));
        txtVariables.setText(text);
    }

    /** עדכון מספר הסייקלים. */
    public void setCycles(long cycles) {
        if (txtCycles != null) {
            txtCycles.setText(Long.toString(cycles));
        }
    }

    /** ניקוי כל הפאנל. */
    public void clear() {
        if (txtVariables != null) txtVariables.clear();
        if (txtCycles != null) txtCycles.clear();
    }

    /** השבתה/הפעלה של הפאנל. */
    public void setDisabled(boolean disabled) {
        if (txtVariables != null) txtVariables.setDisable(disabled);
        if (txtCycles != null) txtCycles.setDisable(disabled);
    }
}
