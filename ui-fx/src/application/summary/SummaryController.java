package application.summary;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SummaryController {

    @FXML private TextField txtBasic;
    @FXML private TextField txtSynthetic;

    @FXML private void initialize() {
        // TODO: set defaults (e.g., "0") / readonly if needed
    }

    public void setBasicCount(long n)     { /* TODO */ }
    public void setSyntheticCount(long n) { /* TODO */ }
    public void clear()                   { /* TODO */ }
}
