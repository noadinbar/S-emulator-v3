package application.inputs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InputsController implements Initializable {

    // שימי לב: ListView של HBox (כל שורה = Label + TextField)
    @FXML private ListView<HBox> lstInputs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // שלד: אין לוגיקה מיוחדת
    }

    /** יוצר שורת קלט: Label + TextField */
    private HBox createRow(String name, String initialValue) {
        Label lbl = new Label(name);
        TextField tf = new TextField();
        if (initialValue != null) tf.setText(initialValue);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        // שהטקסט-פילד יתפרס יפה לרוחב
        HBox.setHgrow(tf, Priority.ALWAYS);

        row.getChildren().addAll(lbl, tf);
        return row;
    }

    /** קובע כמה שורות יהיו ("Input 1", "Input 2", ...) — שלד נקי */
    public void setInputsCount(int count) {
        lstInputs.getItems().clear();
        for (int i = 1; i <= count; i++) {
            lstInputs.getItems().add(createRow("Input " + i, ""));
        }
    }

    /** ממלא ערכים לשורות הקיימות (אם חסרות שורות — משלים) */
    public void setInputValues(List<Long> values) {
        if (values == null) return;
        if (lstInputs.getItems().size() < values.size()) {
            setInputsCount(values.size());
        }
        for (int i = 0; i < values.size(); i++) {
            HBox row = lstInputs.getItems().get(i);
            TextField tf = (TextField) row.getChildren().get(1);
            tf.setText(String.valueOf(values.get(i)));
        }
    }

    /** מחזיר את הערכים כמספרים (לא מספר -> 0) */
    public List<Long> getInputValues() {
        List<Long> out = new ArrayList<>(lstInputs.getItems().size());
        for (HBox row : lstInputs.getItems()) {
            TextField tf = (TextField) row.getChildren().get(1);
            try {
                out.add(Long.parseLong(tf.getText().trim()));
            } catch (NumberFormatException e) {
                out.add(0L);
            }
        }
        return out;
    }

    /** ניקוי הרשימה */
    public void clear() {
        lstInputs.getItems().clear();
    }

    /** השבתה/הפעלה של הקומפוננטה */
    public void setDisabled(boolean disabled) {
        lstInputs.setDisable(disabled);
    }
}
