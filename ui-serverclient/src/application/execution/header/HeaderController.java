package application.execution.header;

import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;

public class HeaderController {

    // --- root/top ---
    @FXML private VBox   executionHeaderRoot;
    @FXML private Label  userNameLabel;
    @FXML private Label  titleLabel;
    @FXML private Label runTargetLabel;
    @FXML private TextField availableCreditsField;

    // --- bottom: exactly like ui-fx ids ---
    @FXML private Button     btnCollapse;
    @FXML private TextField  txtDegree;     // current
    @FXML private TextField  txtMaxDegree;  // max (readonly)
    @FXML private Button     btnExpand;
    @FXML private ComboBox<String> cmbHighlight;

    // callbacks (לוגיקה בחוץ)
    private Runnable onCollapse;
    private Runnable onExpand;
    private Consumer<Integer> onDegreeChanged;
    private Consumer<String> onHighlightChanged;

    private int maxDegree = 0;

    @FXML
    private void initialize() {
        // credits readonly
        availableCreditsField.setEditable(false);
        availableCreditsField.setFocusTraversable(false);

        // מספר שלם בלבד ב-txtDegree (כמו אצלך)
        txtDegree.setTextFormatter(integerOnlyFormatter());
        txtDegree.setText("0");

        txtMaxDegree.setEditable(false);
        txtMaxDegree.setFocusTraversable(false);
        txtMaxDegree.setText("0");

        // highlight: ערכי ברירת-מחדל; אפשר להחליף מבחוץ
        cmbHighlight.getItems().setAll("None", "Instruction", "Block", "Function");
        cmbHighlight.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (onHighlightChanged != null) onHighlightChanged.accept(nv);
        });
    }

    public void setUserName(String name) { userNameLabel.setText(name); }
    public void setAvailableCredits(int credits) { availableCreditsField.setText(Integer.toString(credits)); }

    public void setMaxDegree(int max) {
        this.maxDegree = Math.max(0, max);
        txtMaxDegree.setText(Integer.toString(this.maxDegree));
        int cur = getCurrentDegree();
        if (cur > this.maxDegree) setCurrentDegree(this.maxDegree);
        if (cur < 0) setCurrentDegree(0);
    }

    public int  getCurrentDegree() {
        try { return Integer.parseInt(txtDegree.getText().trim()); }
        catch (Exception e) { return 0; }
    }
    public void setCurrentDegree(int d) { txtDegree.setText(Integer.toString(Math.max(0, Math.min(d, maxDegree)))); }
    public void setRunTarget(String txt) {
        if (runTargetLabel != null) runTargetLabel.setText(txt != null ? txt : "");
    }
    public void setHighlightOptions(List<String> options) {
        cmbHighlight.getItems().setAll(options);
    }
    public void selectHighlight(String value) { cmbHighlight.getSelectionModel().select(value); }

    public void setOnCollapse(Runnable r) { this.onCollapse = r; }
    public void setOnExpand(Runnable r)   { this.onExpand = r; }
    public void setOnDegreeChanged(Consumer<Integer> c) { this.onDegreeChanged = c; }
    public void setOnHighlightChanged(Consumer<String> c){ this.onHighlightChanged = c; }

    // ===== Actions (מחוברות מה-FXML) =====
    @FXML private void onCollapseClicked() { if (onCollapse != null) onCollapse.run(); }
    @FXML private void onExpandClicked()   { if (onExpand   != null) onExpand.run(); }

    // אם תרצי “Apply” נפרד – נוסיף; כרגע שינוי הערך נלכד כשאת קוראת getCurrentDegree/או מאזינה מבחוץ
    @FXML private void onDegreeEdited() {
        if (onDegreeChanged != null) onDegreeChanged.accept(getCurrentDegree());
    }

    // ===== helpers =====
    private static TextFormatter<Integer> integerOnlyFormatter() {
        return new TextFormatter<>(new StringConverter<>() {
            @Override public String toString(Integer object) { return object == null ? "0" : object.toString(); }
            @Override public Integer fromString(String string) {
                if (string == null || string.isBlank()) return 0;
                try { return Integer.parseInt(string.trim()); } catch (NumberFormatException e) { return 0; }
            }
        });
    }
}
