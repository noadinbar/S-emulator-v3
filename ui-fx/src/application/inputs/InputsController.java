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

import display.Command2DTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

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
        if (dto == null || dto.getInputsInUse() == null) return;

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

    /** שורת קלט אחת: Label "Xk =" + TextField מאותחל ל-"0" (מספרים בלבד או ריק). */
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
    }
}
