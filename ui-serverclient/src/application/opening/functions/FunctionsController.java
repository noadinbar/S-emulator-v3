package application.opening.functions;

import application.execution.ExecutionSceneController;
import client.responses.info.FunctionsResponder;
import client.responses.info.StatusResponder;
import com.google.gson.JsonObject;
import display.FunctionRowDTO;
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

public class FunctionsController {

    @FXML private TableView<FunctionRowDTO> functionsTable;
    @FXML private TableColumn<FunctionRowDTO, String>  nameCol;
    @FXML private TableColumn<FunctionRowDTO, String>  programCol;
    @FXML private TableColumn<FunctionRowDTO, String>  uploaderCol;
    @FXML private TableColumn<FunctionRowDTO, Integer> instrCol;
    @FXML private TableColumn<FunctionRowDTO, Integer> degreeCol;
    @FXML private Button executeBtn;

    private final AtomicBoolean shouldUpdate = new AtomicBoolean(true);
    private Timer timer;
    private FunctionsRefresher refresher;
    private String selectedFunctionName;
    private String userName;

    @FXML
    public void initialize() {

        // Bind columns to DTO getters
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        programCol.setCellValueFactory(new PropertyValueFactory<>("programName"));
        uploaderCol.setCellValueFactory(new PropertyValueFactory<>("uploader"));
        instrCol.setCellValueFactory(new PropertyValueFactory<>("baseInstrCount"));
        degreeCol.setCellValueFactory(new PropertyValueFactory<>("maxDegree"));

        // Disable Execute when no selection
        executeBtn.disableProperty().bind(
                functionsTable.getSelectionModel().selectedItemProperty().isNull()
        );

        // Remember current selection whenever user selects a row
        functionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedFunctionName = (newV != null ? newV.getName() : null);
        });
    }

    @FXML
    private void onExecuteAction() {
        FunctionRowDTO sel = functionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String programContextName = sel.getProgramName();
        if (!ensureCreditsPositive(programContextName)) {
            return;
        }
        try {
            ExecutionSceneController controller =
                    openExecutionScene("Execution - Function: " + sel.getName());

            controller.init(ExecTarget.FUNCTION,
                    sel.getName(),
                    sel.getMaxDegree(), sel.getProgramName());
        } catch (Exception ex) {
        }
    }

    public void setUserName(String name) { this.userName = name; }

    public void startFunctionsRefresher() {
        if (timer != null) return;
        refresher = new FunctionsRefresher(shouldUpdate, this::applyRows);
        timer = new Timer(true);
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

    // Replace table content while restoring selection if possible (by function name)
    public void applyRows(List<FunctionRowDTO> rows) {
        final String keep = selectedFunctionName;
        functionsTable.getItems().setAll(rows);

        if (keep != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (keep.equalsIgnoreCase(rows.get(i).getName())) {
                    functionsTable.getSelectionModel().select(i);
                    functionsTable.scrollTo(i);
                    return;
                }
            }
        }
        functionsTable.getSelectionModel().clearSelection();
    }

    // One-shot prime to avoid the first-second lag; uses applyRows
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

                List<display.FunctionRowDTO> rows = FunctionsResponder.rowsParse(rs); // closes rs
                Platform.runLater(() -> applyRows(rows));
            } catch (Exception ignore) {}
        }, "functions-prime").start();
    }
    /**
     * Check that the user has credits > 0 for the parent program of this function.
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
     * Switch current window to the execution scene and return its controller.
     */
    private ExecutionSceneController openExecutionScene(String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/execution/execution_scene.fxml"));
        Parent root = loader.load();
        ExecutionSceneController controller = loader.getController();
        controller.setUserName(userName);
        Stage stage = (Stage) functionsTable.getScene().getWindow();
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
