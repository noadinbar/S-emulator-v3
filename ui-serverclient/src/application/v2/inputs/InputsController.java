package application.v2.inputs;

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
import display.DisplayDTO;
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

    @FXML private ListView<HBox> lstInputs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clear();
    }

    public void show(DisplayDTO dto) {
        clear();
        List<Integer> indices = new ArrayList<>();
        for (VarRefDTO o : dto.getInputsInUse()) {
            if (o.getVariable() == VarOptionsDTO.x) {
                int idx = o.getIndex();
                if (idx > 0) indices.add(idx);
            }
        }

        indices.sort(Comparator.naturalOrder());
        for (Integer idx : indices) {
            lstInputs.getItems().add(createRow(idx));
        }
    }

    public void clear() {
        if (lstInputs != null) lstInputs.getItems().clear();
    }

    private HBox createRow(int index) {
        Label lbl = new Label("x" + index + " =");
        TextField tf = new TextField("0");
        tf.setTextFormatter(new TextFormatter<>(chg ->
                chg.getControlNewText().matches("\\d*") ? chg : null
        ));
        tf.setPrefColumnCount(3);
        HBox row = new HBox(3, lbl, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tf, Priority.NEVER);

        tf.setOnAction(e -> focusNextFromRow(row));
        return row;
    }

    public List<Long> collectValuesPadded() {
        Map<Integer, Long> byIndex = new HashMap<>();
        int maxIndex = 0;

        for (HBox row : lstInputs.getItems()) {
            Label lbl = (Label) row.getChildren().get(0);
            TextField textField = (TextField) row.getChildren().get(1);

            String text = lbl.getText();
            int k = parseIndex(text);
            long value = parseOrZero(textField.getText());

            if (k > 0) {
                byIndex.put(k, value);
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

    public void fillInputs(List<Long> values) {
        if (values == null || lstInputs == null) return;

        for (HBox row : lstInputs.getItems()) {
            Label lbl = (Label) row.getChildren().get(0);
            TextField textField = (TextField) row.getChildren().get(1);

            int k = parseIndex(lbl.getText());  // "x5 =" → 5
            String text = "0";
            if (k > 0 && k - 1 < values.size()) {
                Long value = values.get(k - 1);
                if (value != null) text = String.valueOf(value);
            }
            textField.setText(text);
        }
        focusFirstField();
    }

    private int parseIndex(String labelText) {
        labelText = labelText.trim();
        labelText = labelText.substring(1).trim();

        int i = 0;
        while (i < labelText.length() && Character.isDigit(labelText.charAt(i))) i++;
        try {
            return Integer.parseInt(labelText.substring(0, i));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long parseOrZero(String str) {
        if (str == null || str.isBlank()) return 0L;
        try { return Long.parseLong(str.trim()); }
        catch (NumberFormatException e) { return 0L; }
    }

    public void focusFirstField() {
        if (lstInputs == null || lstInputs.getItems().isEmpty()) return;
        HBox row = lstInputs.getItems().get(0);
        TextField textField = (TextField) row.getChildren().get(1);
        Platform.runLater(() -> { textField.requestFocus(); textField.selectAll(); });
        textField.requestFocus();
    }

    private void focusTextFieldAt(int rowIndex) {
        HBox row = lstInputs.getItems().get(rowIndex);
        TextField textField = (TextField) row.getChildren().get(1);
        Platform.runLater(() -> { textField.requestFocus(); textField.selectAll(); });
    }

    private void focusNextFromRow(HBox currentRow) {
        int i = lstInputs.getItems().indexOf(currentRow);
        if (i >= 0 && i + 1 < lstInputs.getItems().size()) {
            focusTextFieldAt(i + 1);
        }
    }

    public void setInputsEditable(boolean editable) {
        if (lstInputs == null) return;
        for (HBox row : lstInputs.getItems()) {
            TextField textField = (TextField) row.getChildren().get(1);
            textField.setEditable(editable);
            textField.setFocusTraversable(editable);
        }
    }

}
