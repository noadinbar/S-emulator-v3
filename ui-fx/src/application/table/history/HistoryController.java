package application.table.history;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import execution.RunHistoryEntryDTO;

import java.util.List;
import java.util.stream.Collectors;

public class HistoryController {

    @FXML private TableView<RunHistoryEntryDTO> tblHistory;

    @FXML private TableColumn<RunHistoryEntryDTO, Number> colRunId;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colDegree;
    @FXML private TableColumn<RunHistoryEntryDTO, String>  colInputs;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colY;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colCycles;

    private final ObservableList<RunHistoryEntryDTO> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colRunId.setCellValueFactory(cd ->
                new ReadOnlyIntegerWrapper(cd.getValue().getRunNumber()));
        colDegree.setCellValueFactory(cd ->
                new ReadOnlyIntegerWrapper(cd.getValue().getDegree()));
        colInputs.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(toCsv(cd.getValue().getInputs())));
        colY.setCellValueFactory(cd ->
                new ReadOnlyLongWrapper(cd.getValue().getYValue()));
        colCycles.setCellValueFactory(cd ->
                new ReadOnlyLongWrapper(cd.getValue().getCycles()));

        tblHistory.setItems(items);
    }

    public void setEntries(List<RunHistoryEntryDTO> rows) {
        items.setAll(rows);
    }

    public void addEntry(RunHistoryEntryDTO row) {
        items.add(row);
        if (tblHistory != null) {
            tblHistory.scrollTo(items.size() );
        }
    }

    public void clear() {
        items.clear();
    }

    // helper
    private static String toCsv(List<Long> xs) {
        if (xs == null || xs.isEmpty()) return "";
        return xs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
