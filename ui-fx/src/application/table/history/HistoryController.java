package application.table.history;

import execution.debug.DebugStateDTO;
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
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TableRow;
import javafx.util.Duration;


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

    // === BONUS: Row fade-in on new history entry (BEGIN) ===
    // Which row should animate now + a unique "stamp" per addition to avoid double-runs
    private int  animateIndex = -1;
    private long animateStamp = 0;
    // === BONUS: Row fade-in on new history entry (END) ===

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

        // === BONUS: Row fade-in on new history entry (BEGIN) ===
        // RowFactory animates ONLY the cells of the newly added row, once per "stamp"
        tblHistory.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(RunHistoryEntryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;

                if (getIndex() == animateIndex) {
                    // guard: run at most once per stamp, even if updateItem fires again
                    Object done   = getProperties().get("animDoneStamp");
                    Object queued = getProperties().get("animQueuedStamp");
                    if (Long.valueOf(animateStamp).equals(done) || Long.valueOf(animateStamp).equals(queued)) {
                        return;
                    }
                    getProperties().put("animQueuedStamp", animateStamp);

                    // Defer to next pulse so cells exist (handles virtualization + scroll)
                    Platform.runLater(() -> {
                        // If the row scrolled/reused, bail out cleanly
                        if (getIndex() != animateIndex) {
                            getProperties().put("animDoneStamp", animateStamp);
                            getProperties().remove("animQueuedStamp");
                            return;
                        }
                        animateCellsOnce(this  /*ms*/  /*from opacity*/);
                        getProperties().put("animDoneStamp", animateStamp);
                        getProperties().remove("animQueuedStamp");
                        if (getIndex() == animateIndex) {
                            animateIndex = -1;
                        }
                    });
                }
            }
        });
        // === BONUS: Row fade-in on new history entry (END) ===
    }

    // === BONUS: Row fade-in on new history entry (BEGIN) ===
    // Fade-in the row's visible cells (not the background) for a noticeable effect without white gaps
    private void animateCellsOnce(TableRow<?> row) {
        for (Node cell : row.lookupAll(".table-cell")) {
            cell.setOpacity(0.2);
            FadeTransition ft = new FadeTransition(Duration.millis(1200), cell);
            ft.setFromValue(0.2);
            ft.setToValue(1.0);
            ft.setInterpolator(Interpolator.EASE_OUT);
            ft.play();
        }
    }
    // === BONUS: Row fade-in on new history entry (END) ===

    private Consumer<RunHistoryEntryDTO> onRerun;
    public void setOnRerun(Consumer<RunHistoryEntryDTO> cb) { this.onRerun = cb; }
    private Consumer<RunHistoryEntryDTO> onShow;
    public void setOnShow(Consumer<RunHistoryEntryDTO> cb) { this.onShow = cb; }

    public void setEntries(List<RunHistoryEntryDTO> rows) {
        items.setAll(rows);
    }

    public int getTableSize() { return items.size(); }

    public void addEntry(RunHistoryEntryDTO row) {
        items.add(row);
        if (tblHistory != null) {
            // === BONUS: Row fade-in on new history entry (BEGIN) ===
            int index = items.size() - 1;
            animateIndex = index;
            animateStamp++;              // new "stamp" for this addition
            tblHistory.scrollTo(index);  // ensure the row is realized
            tblHistory.layout();         // stabilize VirtualFlow to avoid white flashes
            // === BONUS: Row fade-in on new history entry (END) ===
        }
    }

    public void addEntry(DebugStateDTO state, int degree, List<Long> inputs) {
        if (state == null) return;
        long cycles = state.getCyclesSoFar();
        long y = state.getVars().stream()
                .filter(v -> v.getVar().getVariable() == types.VarOptionsDTO.y)
                .mapToLong(execution.VarValueDTO::getValue)
                .findFirst().orElse(0L);
        addDebugSnapshot(degree, inputs, y, cycles); // משתמשים ב־API קצר פנימי להצגת "צילום"
    }

    public void addDebugSnapshot(int degree, List<Long> inputs, long yValue, long cycles) {
        if (tblHistory == null) return;
        RunHistoryEntryDTO row = new RunHistoryEntryDTO(
                items.size()+1,
                degree,
                inputs,
                yValue,
                (int) cycles
        );
        addEntry(row);
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
