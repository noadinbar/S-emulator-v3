package application.opening.programs;

import application.execution.ExecutionSceneController;
import client.responses.info.ProgramsResponder;
import client.responses.info.StatusResponder;
import com.google.gson.JsonObject;
import display.ProgramRowDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import okhttp3.Request;
import okhttp3.Response;
import utils.Constants;
import utils.ExecTarget;

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
    @FXML private Button executeBtn;

    private final AtomicBoolean shouldUpdate = new AtomicBoolean(true);
    private Timer timer;
    private ProgramsRefresher refresher;
    private String selectedProgramName;
    private String userName;

    @FXML
    public void initialize() {
        // Bind columns to DTO fields (exact names of getters on ProgramRowDTO)
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploader"));
        instrCol.setCellValueFactory(new PropertyValueFactory<>("baseInstrCount"));
        degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));
        runsCol.setCellValueFactory(new PropertyValueFactory<>("numRuns"));
        avgCreditsCol.setCellValueFactory(new PropertyValueFactory<>("avgCredits"));

        executeBtn.disableProperty().bind(
                programsTable.getSelectionModel().selectedItemProperty().isNull()
        );

        programsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedProgramName = (newV != null ? newV.getName() : null);
        });
    }

    @FXML
    private void onExecuteAction() {
        ProgramRowDTO sel = programsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String programContextName = sel.getName();
        if (!ensureCreditsPositive(programContextName)) {
            return;
        }
        try {
            ExecutionSceneController controller =
                    openExecutionScene("Execution - Program: " + sel.getName());
            controller.init(ExecTarget.PROGRAM,
                    sel.getName(),
                    sel.getMaxDegree(), sel.getName());
        } catch (Exception ex) {
        }
    }

    public void setUserName(String name) { this.userName = name; }

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
        final String keep = selectedProgramName;
        programsTable.getItems().setAll(rows);
        if (keep != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (keep.equalsIgnoreCase(rows.get(i).getName())) {
                    programsTable.getSelectionModel().select(i);
                    programsTable.scrollTo(i);
                    return;
                }
            }
        }
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
                        applyRows(rows);
                    });
                }
            } catch (Exception ignore) { }
        }, "programs-prime").start();
    }

    /**
     * Check that the user has credits > 0 for this program.
     * Returns true if allowed to continue into execution.
     * If not allowed, it shows an error popup and returns false.
     */
    private boolean ensureCreditsPositive(String programContextName) {
        try {
            JsonObject js = StatusResponder.get(programContextName);

            if (js != null &&
                    js.has("creditsCurrent") &&
                    !js.get("creditsCurrent").isJsonNull()) {

                int credits = js.get("creditsCurrent").getAsInt();
                if (credits > 0) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        showNoCreditsAlert();
        return false;
    }

    /**
     * Switch current window to the given FXML and return its controller.
     */
    private ExecutionSceneController openExecutionScene(String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/execution/execution_scene.fxml"));
        Parent root = loader.load();
        ExecutionSceneController controller = loader.getController();
        controller.setUserName(userName);
        Stage stage = (Stage) programsTable.getScene().getWindow();
        if (title != null) stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show();
        return controller;
    }

    /**
     * Show popup when user tries to execute with 0 credits.
     */
    private void showNoCreditsAlert() {
        TextArea area = new TextArea("You must charge credits before trying to execute");
        area.setEditable(false);
        area.setWrapText(true);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Insufficient credits");
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
