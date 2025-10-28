package application.opening;

import application.execution.ExecutionSceneController;
import client.requests.runtime.History;
import client.responses.authentication.CreditsResponder;
import client.responses.runtime.HistoryResponder;
import com.google.gson.JsonObject;
import execution.RunHistoryEntryDTO;

import java.io.File;
import java.util.List;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import application.opening.functions.FunctionsController;
import application.opening.header.HeaderController;
import application.opening.programs.ProgramsController;
import application.opening.table.history.HistoryController;
import application.opening.users.UsersController;
import okhttp3.Request;
import utils.ExecTarget;

public class OpeningSceneController {
    @FXML private BorderPane openingRoot;
    @FXML private HeaderController headerController;
    @FXML private UsersController usersController;
    @FXML private ProgramsController programsController;
    @FXML private FunctionsController functionsController;
    @FXML private HistoryController historyController;
    private Window hostWindow;
    private String userName;

    @FXML
    private void initialize() {
        headerController.setOnLocalFileChosen(this::handleLocalFileChosen);
        headerController.setOnChargeCredits(this::handleChargeCredits);
        headerController.refreshStatus();
        headerController.startCreditsRefresher();
        programsController.loadOnceAsync();
        programsController.startProgramsRefresher();
        functionsController.loadOnceAsync();
        functionsController.startFunctionsRefresher();
        usersController.loadOnceAsync();      // see all users immediately
        usersController.startUsersRefresher();
        usersController.setOnUserSelectionChanged(this::handleUserSelectionChanged);
        historyController.setOnShow(this::handleHistoryShow);
        historyController.setOnRerun(this::handleHistoryRerun);
        loadHistoryAsync(null);
    }

    public void setHostWindow(Window window) {
        this.hostWindow = window;
        if (headerController != null) headerController.setHostWindow(window);
    }

    public void setUserName(String name) {
        this.userName = name; // keep logged-in username locally
        if (headerController != null) headerController.setUserName(name);
        if (programsController != null)  programsController.setUserName(name);
        if (functionsController != null) functionsController.setUserName(name);
    }

    public void setAvailableCredits(int credits) {
        if (headerController != null) headerController.setAvailableCredits(credits);
    }

    public void setUsers(List<String> users) {
        if (usersController != null) usersController.setUsers(users);
    }


    public void setHistory(List<RunHistoryEntryDTO> entries) {
        if (historyController != null) historyController.setHistory(entries);
    }

    private void handleLocalFileChosen(File file) {
        // TODO: upload לשרת דרך ClientService → ואז refresh ל-Programs/Functions/History
    }

    /**
     * Trigger a credits charge and refresh the header on success.
     * Disables UI controls during the network call to avoid double-charging.
     */
    private void handleChargeCredits(int amount, Node... nodesToLock) {
        Task<JsonObject> t = new Task<>() {
            @Override protected JsonObject call() throws Exception {
                return CreditsResponder.charge(amount);
            }
        };
        t.setOnSucceeded(ev -> {
            JsonObject js = t.getValue();

            // 1) Update header immediately from the server response
            if (js != null) {
                if (js.has("creditsCurrent") && !js.get("creditsCurrent").isJsonNull()) {
                    headerController.setAvailableCredits(js.get("creditsCurrent").getAsInt());
                }
                if (js.has("username") && !js.get("username").isJsonNull()) {
                    headerController.setUserName(js.get("username").getAsString());
                }
            }

            // 2) Also pull a fresh snapshot (belt & suspenders)
            headerController.refreshStatus();
            headerController.setChargeAmountField("");
        });

        t.setOnFailed(ev -> {
            // You can show an alert/log here with t.getException()
        });

        // Lock UI nodes (e.g., charge button + amount field) during the call
        runAsyncWithUiLock(t, nodesToLock);
    }

    // Called when user clicks "Show" on a history row
    private void handleHistoryShow(RunHistoryEntryDTO row) {
        // Header line: run number
        String header = "Run #" + row.getRunNumber();

        // Body: final snapshot lines (y, x..., z...) taken from outputsSnapshot
        String bodyText = buildSnapshotText(row);

        // Reuse the popup helper
        showInfoPopup("Run Status", header, bodyText);
    }

    private void handleHistoryRerun(RunHistoryEntryDTO row) {
        if (row == null) {
            return;
        }

        try {
            // open the Execution scene window and grab its controller
            ExecutionSceneController execCtrl =
                    openExecutionScene("Execution - Rerun #" + row.getRunNumber());

            // pass username so header credits etc. display correctly
            execCtrl.setUserName(userName);

            // decide if this row represents a PROGRAM or a FUNCTION
            ExecTarget tgt = "FUNCTION".equalsIgnoreCase(row.getTargetType())
                    ? ExecTarget.FUNCTION
                    : ExecTarget.PROGRAM;

            // init() builds the execution scene normally
            execCtrl.init(
                    tgt,
                    row.getTargetName(),   // program name OR function user-string
                    row.getDegree(),       // we can reuse degree as maxDegree for the header
                    row.getTargetName()    // programContextName; using same string is OK here
            );

            // now pass all historical data so the new screen can prefill itself
            execCtrl.prepareFromHistory(
                    row.getDegree(),
                    row.getArchitectureType(),
                    row.getInputs(),
                    row.getRunMode()          // "EXECUTION" or "DEBUG"
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            showInfoPopup(
                    "Rerun failed",
                    null,
                    ex.getMessage() == null ? "Unknown error" : ex.getMessage()
            );
        }
    }

    /**
     * Disables given UI nodes while the task is running, then re-enables them.
     * Also starts the task on a background thread.
     */
    private void runAsyncWithUiLock(Task<?> task, Node... toDisable) {
        for (Node n : toDisable) {
            if (n != null) n.setDisable(true);
        }

        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, ev -> {
            for (Node n : toDisable) {
                if (n != null) n.setDisable(false);
            }
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, ev -> {
            for (Node n : toDisable) {
                if (n != null) n.setDisable(false);
            }
        });

        Thread t = new Thread(task, "charge-credits");
        t.setDaemon(true);
        t.start();
    }

    private void handleUserSelectionChanged(String usernameOrNull) {
        loadHistoryAsync(usernameOrNull);
    }

    private void loadHistoryAsync(String usernameOrNull) {
        Task<List<RunHistoryEntryDTO>> t =
                new Task<>() {
                    @Override
                    protected List<RunHistoryEntryDTO> call() throws Exception {
                        Request req = History.build(usernameOrNull);
                        return HistoryResponder.get(req);
                    }
                };

        t.setOnSucceeded(ev -> {
            List<RunHistoryEntryDTO> data = t.getValue();
            if (historyController != null) {
                Platform.runLater(() -> historyController.setHistory(data));
            }
        });

        t.setOnFailed(ev -> {
            if (historyController != null) {
                Platform.runLater(() -> historyController.clear());
            }
        });
        Thread bg = new Thread(t, "history-load");
        bg.setDaemon(true);
        bg.start();
    }

    public void stopAllRefreshers() {
        programsController.stopProgramsRefresher();
        functionsController.stopFunctionsRefresher();
        usersController.stopUsersRefresher();
    }

    // Build the text body that will be shown in the popup
    // Uses the pre-formatted snapshot from the server (y first, then x, then z)
    private String buildSnapshotText(RunHistoryEntryDTO row) {
        List<String> snapshot = row.getOutputsSnapshot();
        if (snapshot.isEmpty()) {
            return "No snapshot available.";
        }

        StringBuilder sb = new StringBuilder();
        for (String line : snapshot) {
            sb.append(line).append("\n");
        }

        // trim to avoid trailing newline in the alert
        return sb.toString().trim();
    }

    private void showInfoPopup(String title, String header, String bodyText) {
        TextArea area = new TextArea(bodyText);
        area.setEditable(false);
        area.setWrapText(true);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private ExecutionSceneController openExecutionScene(String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/execution/execution_scene.fxml"));
        Parent root = loader.load();
        ExecutionSceneController controller = loader.getController();
        if (userName != null) {
            controller.setUserName(userName);
        }

        Stage stage = (Stage) openingRoot.getScene().getWindow();
        if (title != null) {
            stage.setTitle(title);
        }
        stage.setScene(new Scene(root));
        stage.show();
        return controller;
    }
}
