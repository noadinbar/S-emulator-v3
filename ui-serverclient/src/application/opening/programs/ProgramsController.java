package application.opening.programs;

import client.responses.ProgramsResponder;
import display.ProgramRowDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import okhttp3.Request;
import okhttp3.Response;
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

    public void loadOnceAsync() {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(Constants.BASE_URL + Constants.API_PROGRAMS)
                        .get()
                        .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                        .build();

                try (Response rs = utils.HttpClientUtil.runSync(req)) {
                    if (!rs.isSuccessful()) return;
                    List<display.ProgramRowDTO> rows = ProgramsResponder.parse(rs);
                    Platform.runLater(() -> {
                        programsTable.getItems().setAll(rows);
                    });
                }
            } catch (Exception ignore) { }
        }, "programs-prime").start();
    }
}
