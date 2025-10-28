package application.opening.table.history;

import execution.RunHistoryEntryDTO;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class HistoryController {

    @FXML private TableView<RunHistoryEntryDTO> tblHistory;

    @FXML private TableColumn<RunHistoryEntryDTO, Number> colRunId;
    @FXML private TableColumn<RunHistoryEntryDTO, String> colType;
    @FXML private TableColumn<RunHistoryEntryDTO, String> colName;
    @FXML private TableColumn<RunHistoryEntryDTO, String> colArch;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colDegree;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colY;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colCycles;
    @FXML private Button btnRerun;
    @FXML private Button btnShow;

    private final ObservableList<RunHistoryEntryDTO> items = FXCollections.observableArrayList();
    private Consumer<RunHistoryEntryDTO> onShowClicked;
    private Consumer<RunHistoryEntryDTO> onRerunClicked;

    @FXML
    private void initialize() {
        tblHistory.setItems(items);
        colRunId.setCellValueFactory(cd ->
                new ReadOnlyIntegerWrapper(cd.getValue().getRunNumber()));

        colType.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getTargetType()));

        colName.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getTargetName()));

        colArch.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getArchitectureType()));

        colDegree.setCellValueFactory(cd ->
                new ReadOnlyIntegerWrapper(cd.getValue().getDegree()));

        colY.setCellValueFactory(cd ->
                new ReadOnlyLongWrapper(cd.getValue().getFinalY()));

        colCycles.setCellValueFactory(cd ->
                new ReadOnlyLongWrapper(cd.getValue().getCyclesCount()));

        // disable buttons when no history row is selected
        btnShow.disableProperty().bind(
                tblHistory.getSelectionModel().selectedItemProperty().isNull()
        );
        btnRerun.disableProperty().bind(
                tblHistory.getSelectionModel().selectedItemProperty().isNull()
        );
    }

    @FXML
    private void onShowAction() {
        RunHistoryEntryDTO row = tblHistory.getSelectionModel().getSelectedItem();
        if (row != null && onShowClicked != null) {
            onShowClicked.accept(row);
        }
    }

    @FXML
    private void onRerunAction() {
        RunHistoryEntryDTO row = tblHistory.getSelectionModel().getSelectedItem();
        if (row != null && onRerunClicked != null) {
            onRerunClicked.accept(row);
        }
    }

    public void setHistory(List<RunHistoryEntryDTO> list) {
        items.setAll(list);
    }

    public void setOnShow(Consumer<RunHistoryEntryDTO> cb) {
        this.onShowClicked = cb;
    }

    public void setOnRerun(Consumer<RunHistoryEntryDTO> cb) {
        this.onRerunClicked = cb;
    }

    public void clear() {
        items.clear();
    }

    public TableView<RunHistoryEntryDTO> getTableView() {
        return tblHistory;
    }

    public void addEntry(RunHistoryEntryDTO row) {
        items.add(row);
    }


    public static String inputsAsCsv(List<Long> xs) {
        if (xs == null || xs.isEmpty()) {
            return "";
        }
        return xs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
