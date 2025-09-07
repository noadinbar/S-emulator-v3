package application.inputs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.application.Platform;
import display.Command2DTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class InputsController implements Initializable {

    // כל פריט ברשימה: HBox עם Label "Xk =" + TextField עריכתי
    @FXML private ListView<HBox> lstInputs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clear();
    }

    /** מציג שורות לפי ה-DTO: קורא X-ים מתוך getInputsInUse (VarRefDTO) ומייצר Xk = */
    public void show(Command2DTO dto) {
        clear();

        List<Integer> indices = new ArrayList<>();
        for (Object o : dto.getInputsInUse()) {
            VarRefDTO v = (VarRefDTO) o;               // הרשימה אצלך היא VarRefDTO
            if (v.getVariable() == VarOptionsDTO.x) {
                int idx = v.getIndex();
                if (idx > 0) indices.add(idx);
            }
        }

        indices.sort(Comparator.naturalOrder());
        for (Integer idx : indices) {
            lstInputs.getItems().add(createRow(idx));  // Xk = + TextField מאותחל ל-"0"
        }
    }

    /** ניקוי הרשימה. */
    public void clear() {
        if (lstInputs != null) lstInputs.getItems().clear();
    }

    // ---------- עוזר פנימי ----------

    /** שורת קלט אחת: Label "Xk =" + TextField מאותחל ל-"0" (מספרים בלבד או ריק).
    private HBox createRow(int index) {
        Label lbl = new Label("x" + index + " =");

        TextField tf = new TextField("0"); // ערך התחלתי; המשתמש יכול לערוך
        // מאפשר רק ספרות; ריק מותר (בהמשך יפורש כ-0 כשנאסוף ערכים)
        tf.setTextFormatter(new TextFormatter<>(chg ->
                chg.getControlNewText().matches("\\d*") ? chg : null
        ));

        tf.setPrefColumnCount(3);                 // רוחב לפי מספר תווים
        HBox.setHgrow(tf, Priority.NEVER);

        HBox row = new HBox(8, lbl, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }*/
    private HBox createRow(int index) {
        Label lbl = new Label("X" + index + " =");
        TextField tf = new TextField("0");
        tf.setTextFormatter(new TextFormatter<>(chg ->
                chg.getControlNewText().matches("\\d*") ? chg : null
        ));
        tf.setPrefColumnCount(3);
        HBox row = new HBox(8, lbl, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tf, Priority.NEVER);

        tf.setOnAction(e -> focusNextFromRow(row)); // Enter -> לשורה הבאה
        return row;
    }

    // === PUBLIC API: איסוף ערכי הקלט מה-UI ===

    /** אוסף ערכים מהטקסט-פילדים לפי האינדקס בלייבל (Xk =), מרפד באפסים עד המקסימום, ומחזיר ברשימה. */
    public List<Long> collectValuesPadded() {
        Map<Integer, Long> byIndex = new HashMap<>();
        int maxIndex = 0;

        for (HBox row : lstInputs.getItems()) {
            Label lbl = (Label) row.getChildren().get(0);
            TextField tf = (TextField) row.getChildren().get(1);

            String text = lbl.getText();
            int k = parseIndex(text);
            long val = parseOrZero(tf.getText());

            if (k > 0) {
                byIndex.put(k, val);
                if (k > maxIndex) maxIndex = k;
            }
        }

        List<Long> padded = new ArrayList<>(maxIndex);
        for (int i = 1; i <= maxIndex; i++) {
            padded.add(byIndex.getOrDefault(i, 0L));
        }
        return padded;
    }

    public String collectValuesCsvPadded() {
        List<Long> vals = collectValuesPadded();
        return vals.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

// === helpers מקומיים ===

    private int parseIndex(String labelText) {

        labelText = labelText.trim();

        labelText = labelText.substring(1).trim(); // "12 =" → "12 ="

        // קוטם כל מה שלא ספרה בסוף
        int i = 0;
        while (i < labelText.length() && Character.isDigit(labelText.charAt(i))) i++;

        try {
            return Integer.parseInt(labelText.substring(0, i));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long parseOrZero(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    public void focusFirstField() {
        if (lstInputs == null || lstInputs.getItems().isEmpty()) return;
        HBox row = lstInputs.getItems().get(0);
        TextField tf = (TextField) row.getChildren().get(1);
        Platform.runLater(() -> { tf.requestFocus(); tf.selectAll(); });
    }

    private void focusTextFieldAt(int rowIndex) {
        HBox row = lstInputs.getItems().get(rowIndex);
        TextField tf = (TextField) row.getChildren().get(1);
        Platform.runLater(() -> { tf.requestFocus(); tf.selectAll(); });
    }

    private void focusNextFromRow(HBox currentRow) {
        int i = lstInputs.getItems().indexOf(currentRow);
        if (i >= 0 && i + 1 < lstInputs.getItems().size()) {
            focusTextFieldAt(i + 1);
        }
    }






}
