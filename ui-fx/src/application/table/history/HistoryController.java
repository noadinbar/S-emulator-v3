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
import javafx.scene.control.Button;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HistoryController {

    @FXML private TableView<RunHistoryEntryDTO> tblHistory;

    @FXML private TableColumn<RunHistoryEntryDTO, Number> colRunId;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colDegree;
    @FXML private TableColumn<RunHistoryEntryDTO, String>  colInputs;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colY;
    @FXML private TableColumn<RunHistoryEntryDTO, Number> colCycles;
    @FXML private Button btnRerun;
    @FXML private Button btnShow;

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

        btnRerun.disableProperty().bind(
                tblHistory.getSelectionModel().selectedItemProperty().isNull());

        if (btnShow != null) {
            btnShow.disableProperty().bind(
                    tblHistory.getSelectionModel().selectedItemProperty().isNull());
        }
    }


    private Consumer<RunHistoryEntryDTO> onRerun;
    public void setOnRerun(Consumer<RunHistoryEntryDTO> cb) { this.onRerun = cb; }
    private Consumer<RunHistoryEntryDTO> onShow;
    public void setOnShow(Consumer<RunHistoryEntryDTO> cb) { this.onShow = cb; }

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
    public TableView<RunHistoryEntryDTO> getTableView() { return tblHistory; }

    private static String toCsv(List<Long> xs) {
        if (xs == null || xs.isEmpty()) return "";
        return xs.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @FXML
    private void onRerunAction() {
        RunHistoryEntryDTO row = (tblHistory != null)
                ? tblHistory.getSelectionModel().getSelectedItem()
                : null;
        if (row != null && onRerun != null) {
            onRerun.accept(row);
        }
    }

    @FXML
    private void onShowAction() {
        RunHistoryEntryDTO row = (tblHistory != null)
                ? tblHistory.getSelectionModel().getSelectedItem()
                : null;
        if (row != null && onShow != null) {
            onShow.accept(row);
        }
    }

}
