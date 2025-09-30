package application.outputs;

import execution.ExecutionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import types.VarOptionsDTO;

import java.util.*;
import java.util.stream.Collectors;

public class OutputsController {

    @FXML
    private VBox linesBox;
    @FXML
    private TextField txtCycles;

    private final Map<String, javafx.scene.control.Label> varLabels = new HashMap<>();


    @FXML
    private void initialize() {
        if (txtCycles != null) {
            txtCycles.setEditable(false);
        }
    }

    /**
     * lines as non-editable labels.
     */
    public void setVariableLines(List<String> lines) {
        if (linesBox == null) return;
        linesBox.getChildren().clear();
        if (lines == null || lines.isEmpty()) return;

        for (String str : lines) {
            String text = (str == null) ? "" : str.trim();
            Label line = new Label(" " + text); // leading space as requested
            line.getStyleClass().add("out-line"); // CSS hook for coloring whole line later

            String varName = text;
            int eq = text.indexOf('=');
            if (eq > 0) varName = text.substring(0, eq).trim(); // "y", "x1", "z2"...
            line.setUserData(varName);
            varLabels.put(varName, line);

            linesBox.getChildren().add(line);
        }
    }

    public void highlightChanged(Set<String> changedNames) {
        if (linesBox == null) return;

        for (Label lbl : varLabels.values()) {
            lbl.getStyleClass().remove("var-changed");
            lbl.setStyle("");
        }

        if (changedNames == null) return;

        for (String name : changedNames) {
            javafx.scene.control.Label lbl = varLabels.get(name);
            if (lbl != null) {
                lbl.getStyleClass().add("var-changed");
                lbl.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        }
    }

    public List<String> getVariableLines() {
        if (linesBox == null) return null;
        return linesBox.getChildren().stream()
                .filter(n -> n instanceof Label)
                .map(n -> ((Label) n).getText())
                .collect(Collectors.toList());
    }

    public void setCycles(long cycles) {
        if (txtCycles != null) {
            txtCycles.setText(Long.toString(cycles));
        }
    }

    private static String formatFinalsForDisplay(ExecutionDTO result) {
        StringBuilder stringB = new StringBuilder();

        stringB.append("y = ").append(result.getyValue()).append('\n');

        result.getFinals().stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.x)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .forEach(v -> stringB.append('x')
                        .append(v.getVar().getIndex())
                        .append(" = ")
                        .append(v.getValue())
                        .append('\n'));

        result.getFinals().stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.z)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .forEach(v -> stringB.append('z')
                        .append(v.getVar().getIndex())
                        .append(" = ")
                        .append(v.getValue())
                        .append('\n'));

        return stringB.toString().trim();
    }

    public void showExecution(ExecutionDTO result) {
        if (txtCycles != null) {
            txtCycles.setText(Long.toString(result.getTotalCycles()));
        }
        if (linesBox != null) {
            String text = formatFinalsForDisplay(result);
            List<String> lines = Arrays.asList(text.split("\\R"));
            setVariableLines(lines);
        }
    }

    public void clear() {
        if (linesBox != null) linesBox.getChildren().clear();
        if (txtCycles != null) txtCycles.clear();
    }
}