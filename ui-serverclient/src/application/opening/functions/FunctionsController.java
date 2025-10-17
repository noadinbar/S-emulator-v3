package application.opening.functions;

import display.FunctionRowDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.Constants;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

/** Controller for the Functions table (Section 10). */
public class FunctionsController {

    @FXML private TableView<FunctionRowDTO> functionsTable;
    @FXML private TableColumn<FunctionRowDTO, String>  nameCol;
    @FXML private TableColumn<FunctionRowDTO, String>  programCol;
    @FXML private TableColumn<FunctionRowDTO, String>  uploaderCol;
    @FXML private TableColumn<FunctionRowDTO, Integer> instrCol;
    @FXML private TableColumn<FunctionRowDTO, Integer> degreeCol;

    // --- added: polling infra ---
    private final AtomicBoolean shouldUpdate = new AtomicBoolean(true);
    private Timer timer;
    private FunctionsRefresher refresher;

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploader"));
        instrCol.setCellValueFactory(new PropertyValueFactory<>("baseInstrCount"));
        degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
    }

    /** Replace table content with given rows. */
    public void applyRows(List<FunctionRowDTO> rows) {
        functionsTable.getItems().setAll(rows);
    }

    public void startFunctionsRefresher() {
        if (timer != null) return;
        refresher = new FunctionsRefresher(shouldUpdate, this::applyRows);
        timer = new Timer(true); // daemon
        timer.schedule(refresher, Constants.REFRESH_RATE_MS, Constants.REFRESH_RATE_MS);
    }

    public void stopFunctionsRefresher() {
        shouldUpdate.set(false);
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @FXML
    private void onExecuteAction() {
        functionsTable.getSelectionModel().clearSelection();
    }
}
