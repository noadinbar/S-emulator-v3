package application.execution.summary;

import application.execution.table.instruction.InstructionsController;
import display.InstructionDTO;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SummaryController {

    @FXML private TextField txtBasic;
    @FXML private TextField txtSynthetic;

    @FXML
    private void initialize() {

        setDisabled(true);
        clearText();
    }

    private void setDisabled(boolean disabled) {
        if (txtBasic != null)    txtBasic.setDisable(disabled);
        if (txtSynthetic != null) txtSynthetic.setDisable(disabled);
    }

    private void setCounts(long basic, long synthetic) {
        if (txtBasic != null)    txtBasic.setText(Long.toString(basic));
        if (txtSynthetic != null) txtSynthetic.setText(Long.toString(synthetic));
    }

    private void clearText() {
        if (txtBasic != null)    txtBasic.clear();
        if (txtSynthetic != null) txtSynthetic.clear();
    }

    public void wireTo(InstructionsController tableCtrl) {
        if (tableCtrl == null) return;

        ObservableList<InstructionDTO> items = tableCtrl.getItemsView();

        Runnable recalc = () -> {
            boolean empty = items == null || items.isEmpty();
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> applyState(tableCtrl, empty));
            } else {
                applyState(tableCtrl, empty);
            }
        };

        recalc.run();
        if (items != null) {
            items.addListener((ListChangeListener<InstructionDTO>) c -> recalc.run());
        }
    }

    private void applyState(InstructionsController tableCtrl, boolean empty) {
        if (empty) {
            setDisabled(true);
            clearText();
        } else {
            setDisabled(false);
            setCounts(tableCtrl.countBasic(), tableCtrl.countSynthetic());
        }
    }
}
