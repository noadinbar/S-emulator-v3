package application.header;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class HeaderController {

    // עליון
    @FXML private Button btnLoad;
    @FXML private TextField txtPath;

    // תחתון (משמאל לימין)
    @FXML private ComboBox<String> cmbProgram;
    @FXML private Button btnCollapse;
    @FXML private TextField txtDegree;     // <<< במקום Label
    @FXML private Button btnExpand;
    @FXML private ComboBox<String> cmbHighlight;

    @FXML
    private void initialize() {
        txtPath.setEditable(false);
        txtDegree.setEditable(false);
        txtDegree.setFocusTraversable(false);
        txtDegree.setText("0 / 0");

        cmbProgram.setDisable(true);
        btnCollapse.setDisable(true);
        btnExpand.setDisable(true);
        cmbHighlight.setDisable(true);
    }

    // Handlers
    @FXML private void onLoadClicked()      { /* בהמשך */ }
    @FXML private void onProgramChanged()   { /* בהמשך */ }
    @FXML private void onCollapseClicked()  { /* בהמשך */ }
    @FXML private void onExpandClicked()    { /* בהמשך */ }
    @FXML private void onHighlightChanged() { /* בהמשך */ }

    /** קריאה מה-Controller הראשי לעדכון הטקסט: */
    public void setDegree(int current, int max) {
        txtDegree.setText(current + " / " + max);
    }
}
