package application.opening.programs;

import application.execution.ExecutionSceneController;
import client.responses.info.ProgramsResponder;
import display.ProgramRowDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

        try {
            ExecutionSceneController controller =
                    openExecutionScene("Execution - Program: " + sel.getName());
            controller.init(ExecTarget.PROGRAM,
                    sel.getName(),
                    sel.getMaxDegree(), sel.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
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
}
