package application.outputs;

import execution.ExecutionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import types.VarOptionsDTO;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OutputsController {

    @FXML private VBox linesBox;
    @FXML private TextField txtCycles;

    @FXML
    private void initialize() {
        if (txtCycles != null) {
            txtCycles.setEditable(false);
        }
    }

    /**lines as non-editable labels.*/
    public void setVariableLines(List<String> lines) {
        if (linesBox == null) return;
        linesBox.getChildren().clear();
        if (lines == null || lines.isEmpty()) return;

        for (String str : lines) {
            String text = (str == null) ? "" : str.trim();
            Label line = new Label(" " + text); // leading space as requested
            line.getStyleClass().add("out-line"); // CSS hook for coloring whole line later
            linesBox.getChildren().add(line);
        }
    }


    public void setVariables(Map<String, Long> vars) {
        if (linesBox == null) return;
        linesBox.getChildren().clear();
        if (vars == null || vars.isEmpty()) return;

        List<String> lines = vars.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.toList());
        setVariableLines(lines);
    }

    /** Update the cycle count text. */
    public void setCycles(long cycles) {
        if (txtCycles != null) {
            txtCycles.setText(Long.toString(cycles));
        }
    }

    /**
     * y first, then all x in ascending index, then all z in ascending index.
     * builds a single multi-line string, which we split into labels per line.
     */
    private static String formatFinalsForDisplay(ExecutionDTO result) {
        StringBuilder stringB = new StringBuilder();

        // 1) y first
        stringB.append("y = ").append(result.getyValue()).append('\n');

        // 2) all x by ascending index
        result.getFinals().stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.x)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .forEach(v -> stringB.append('x')
                        .append(v.getVar().getIndex())
                        .append(" = ")
                        .append(v.getValue())
                        .append('\n'));

        // 3) all z by ascending index
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

    /**
     * Show execution results: cycles + variables.
     * Uses formatting, then splits into single-label lines.
     */
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

    /** Clear all output. */
    public void clear() {
        if (linesBox != null) linesBox.getChildren().clear();
        if (txtCycles != null) txtCycles.clear();
    }

    /** Enable/disable the whole outputs panel. */
    public void setDisabled(boolean disabled) {
        if (linesBox != null) linesBox.setDisable(disabled);
        if (txtCycles != null) txtCycles.setDisable(disabled);
    }
}
