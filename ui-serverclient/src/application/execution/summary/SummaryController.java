package application.execution.summary;

import application.execution.table.instruction.InstructionsController;
import display.InstructionDTO;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class SummaryController {

    // Existing summary fields
    @FXML private TextField txtBasic;
    @FXML private TextField txtSynthetic;

    // New architecture summary (I..IV)
    @FXML private Label lblGenI;
    @FXML private Label lblGenII;
    @FXML private Label lblGenIII;
    @FXML private Label lblGenIV;
    @FXML private TextField txtGenI;
    @FXML private TextField txtGenII;
    @FXML private TextField txtGenIII;
    @FXML private TextField txtGenIV;

    private String selectedArchitecture = null; // "I".."IV" or null (no selection)

    @FXML
    private void initialize() {
        setDisabled(true);
        clearText();
        clearHighlightStyles();
    }

    /** Called by the parent controller whenever the user picks an architecture in the combo box. */
    public void setSelectedArchitecture(String arch) {
        // Normalize to I/II/III/IV or null
        selectedArchitecture = normalizeArch(arch);
        // Re-evaluate highlighting immediately (counts may already be present)
        updateHighlights();
    }

    private String normalizeArch(String arch) {
        if (arch == null) return null;
        String t = arch.trim().toUpperCase();
        return switch (t) {
            case "I", "II", "III", "IV" -> t;
            default -> null;
        };
    }

    private void setDisabled(boolean disabled) {
        if (txtBasic != null)    txtBasic.setDisable(disabled);
        if (txtSynthetic != null) txtSynthetic.setDisable(disabled);

        if (txtGenI != null) txtGenI.setDisable(disabled);
        if (txtGenII != null) txtGenII.setDisable(disabled);
        if (txtGenIII != null) txtGenIII.setDisable(disabled);
        if (txtGenIV != null) txtGenIV.setDisable(disabled);

        if (lblGenI != null) lblGenI.setDisable(disabled);
        if (lblGenII != null) lblGenII.setDisable(disabled);
        if (lblGenIII != null) lblGenIII.setDisable(disabled);
        if (lblGenIV != null) lblGenIV.setDisable(disabled);
    }

    private void clearText() {
        if (txtBasic != null) txtBasic.setText("0");
        if (txtSynthetic != null) txtSynthetic.setText("0");

        if (txtGenI != null) txtGenI.setText("0");
        if (txtGenII != null) txtGenII.setText("0");
        if (txtGenIII != null) txtGenIII.setText("0");
        if (txtGenIV != null) txtGenIV.setText("0");
    }

    private void clearHighlightStyles() {
        // Reset styles on all labels and fields
        setRed(lblGenI, txtGenI, false);
        setRed(lblGenII, txtGenII, false);
        setRed(lblGenIII, txtGenIII, false);
        setRed(lblGenIV, txtGenIV, false);
    }

    private void setCounts(long basic, long synthetic, long g1, long g2, long g3, long g4) {
        if (txtBasic != null)    txtBasic.setText(Long.toString(basic));
        if (txtSynthetic != null) txtSynthetic.setText(Long.toString(synthetic));

        if (txtGenI != null) txtGenI.setText(Long.toString(g1));
        if (txtGenII != null) txtGenII.setText(Long.toString(g2));
        if (txtGenIII != null) txtGenIII.setText(Long.toString(g3));
        if (txtGenIV != null) txtGenIV.setText(Long.toString(g4));

        // After counts change, re-evaluate highlights
        updateHighlights();
    }

    /** Wire this summary to follow the main program table. */
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
            clearHighlightStyles();
        } else {
            setDisabled(false);

            List<InstructionDTO> list = tableCtrl.getItemsView();
            long basic = list.stream().filter(i -> i.getKind() != null && i.getKind().name().equals("BASIC")).count();
            long synthetic = list.stream().filter(i -> i.getKind() != null && i.getKind().name().equals("SYNTHETIC")).count();

            long g1 = countGen(list, "I");
            long g2 = countGen(list, "II");
            long g3 = countGen(list, "III");
            long g4 = countGen(list, "IV");

            setCounts(basic, synthetic, g1, g2, g3, g4);
        }
    }

    private long countGen(List<InstructionDTO> list, String g) {
        return list.stream()
                .map(InstructionDTO::getGeneration)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> s.equals(g))
                .count();
    }

    private void updateHighlights() {
        // Rule: highlight in red any architecture STRICTLY GREATER than the selected one AND whose count > 0.
        // If no selection -> clear all highlights.
        clearHighlightStyles();
        if (selectedArchitecture == null) return;

        int sel = rank(selectedArchitecture);
        applyHighlightFor(txtGenI, lblGenI, 1, sel);
        applyHighlightFor(txtGenII, lblGenII, 2, sel);
        applyHighlightFor(txtGenIII, lblGenIII, 3, sel);
        applyHighlightFor(txtGenIV, lblGenIV, 4, sel);
    }

    private void applyHighlightFor(TextField field, Label label, int archRank, int selectedRank) {
        if (field == null || label == null) return;
        // parse count
        long count = 0;
        try {
            count = Long.parseLong(field.getText() == null ? "0" : field.getText().trim());
        } catch (NumberFormatException ignore) { /* keep 0 */ }
        boolean shouldRed = (archRank > selectedRank) && (count > 0);
        setRed(label, field, shouldRed);
    }

    private void setRed(Label lbl, TextField tf, boolean on) {
        String style = on ? "-fx-text-fill: red; -fx-font-weight: bold;" : "";
        if (lbl != null) lbl.setStyle(style);
        if (tf != null) tf.setStyle(style);
    }

    private int rank(String arch) {
        return switch (arch) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            default -> 0;
        };
    }
}
