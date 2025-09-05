package application.table.instruction;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class InstructionsController {

    @FXML private TableView<InstructionRow> tblInstructions;

    @FXML private TableColumn<InstructionRow, Number> colLine;
    @FXML private TableColumn<InstructionRow, String>  colBS;
    @FXML private TableColumn<InstructionRow, String>  colLabel;
    @FXML private TableColumn<InstructionRow, Number> colCycles;
    @FXML private TableColumn<InstructionRow, String>  colInstruction;

    @FXML private void initialize() {
        // TODO: set items & cell value factories later
    }

    // stubs
    public void setRows(java.util.List<InstructionRow> rows) { /* TODO */ }
    public void addRow(InstructionRow row) { /* TODO */ }
    public void clear() { /* TODO */ }

    // row model (stubs)
    public static class InstructionRow {
        // TODO: getters/properties: line(int), bs(String), label(String), cycles(long), instruction(String)
    }
}