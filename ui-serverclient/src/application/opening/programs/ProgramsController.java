package application.opening.programs;

import display.ProgramRowDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.Constants;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgramsController {

    @FXML private TableView<ProgramRowDTO> programsTable;
    @FXML private TableColumn<ProgramRowDTO, String>  nameCol;
    @FXML private TableColumn<ProgramRowDTO, String>  uploaderCol;
    @FXML private TableColumn<ProgramRowDTO, Integer> instrCol;
    @FXML private TableColumn<ProgramRowDTO, Integer> degreeCol;
    @FXML private TableColumn<ProgramRowDTO, Integer> runsCol;
    @FXML private TableColumn<ProgramRowDTO, Double>  avgCreditsCol;

    private final AtomicBoolean shouldUpdate = new AtomicBoolean(true);
    private Timer timer;
    private ProgramsRefresher refresher;

    @FXML
    public void initialize() {
        // Bind columns to DTO fields (exact names of getters on ProgramRowDTO)
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploader"));
        instrCol.setCellValueFactory(new PropertyValueFactory<>("baseInstrCount"));
        degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
        runsCol.setCellValueFactory(new PropertyValueFactory<>("numRuns"));
        avgCreditsCol.setCellValueFactory(new PropertyValueFactory<>("avgCredits"));
    }

    public void startProgramsRefresher() {
        if (timer != null) return;
        refresher = new ProgramsRefresher(shouldUpdate, this::applyRows);
        timer = new Timer(true);
        timer.schedule(refresher, Constants.REFRESH_RATE_MS, Constants.REFRESH_RATE_MS);
    }

    public void stopProgramsRefresher() {
        shouldUpdate.set(false);
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void applyRows(List<ProgramRowDTO> rows) {
        programsTable.getItems().setAll(rows);
    }

    @FXML
    private void onExecuteAction() {
        programsTable.getSelectionModel().clearSelection();
    }
}
