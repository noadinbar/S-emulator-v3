package application.table.history;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class HistoryController {

    @FXML private TableView<HistoryRow> tblHistory;

    @FXML private TableColumn<HistoryRow, Number> colRunId;
    @FXML private TableColumn<HistoryRow, Number> colDegree;
    @FXML private TableColumn<HistoryRow, String>  colInputs;
    @FXML private TableColumn<HistoryRow, Number> colY;
    @FXML private TableColumn<HistoryRow, Number> colCycles;

    private final ObservableList<HistoryRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colRunId.setCellValueFactory(new PropertyValueFactory<>("runId"));
        colDegree.setCellValueFactory(new PropertyValueFactory<>("degree"));
        colInputs.setCellValueFactory(new PropertyValueFactory<>("inputs"));
        colY.setCellValueFactory(new PropertyValueFactory<>("y"));
        colCycles.setCellValueFactory(new PropertyValueFactory<>("cycles"));

        tblHistory.setItems(items);
    }

    // stubs
    public void setEntries(java.util.List<HistoryRow> rows) { /* TODO */ }
    public void addEntry(HistoryRow row) { /* TODO */ }
    public void clear() { /* TODO */ }

    public static class HistoryRow {
        // TODO: fields + getters: int runId, int degree, String inputs, long y, long cycles
        // e.g. public int getRunId(){return runId;} etc.
    }
}