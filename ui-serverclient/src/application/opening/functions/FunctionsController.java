package application.opening.functions;

import client.responses.FunctionsResponder;
import display.FunctionRowDTO;
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

    public void loadOnceAsync() {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(Constants.BASE_URL + Constants.API_FUNCTIONS)
                        .get()
                        .addHeader(Constants.HEADER_ACCEPT, Constants.CONTENT_TYPE_JSON)
                        .build();

                Response rs = utils.HttpClientUtil.runSync(req);
                if (!rs.isSuccessful()) { rs.close(); return; }

                List<display.FunctionRowDTO> rows = FunctionsResponder.rowsParse(rs); // rowsParse סוגרת את rs
                Platform.runLater(() -> {
                    functionsTable.getItems().setAll(rows);
                });
            } catch (Exception ignore) {}
        }, "functions-prime").start();
    }
}
