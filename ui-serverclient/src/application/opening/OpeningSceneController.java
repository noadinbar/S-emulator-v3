package application.opening;

import client.responses.authentication.CreditsResponder;
import com.google.gson.JsonObject;
import execution.RunHistoryEntryDTO;

import java.io.File;
import java.util.List;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import application.opening.functions.FunctionsController;
import application.opening.header.HeaderController;
import application.opening.programs.ProgramsController;
import application.opening.table.history.HistoryController;
import application.opening.users.UsersController;

public class OpeningSceneController {
    @FXML private BorderPane openingRoot;
    @FXML private HeaderController headerController;
    @FXML private UsersController usersController;
    @FXML private ProgramsController programsController;
    @FXML private FunctionsController functionsController;
    @FXML private HistoryController historyController;

    private Window hostWindow;

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
    }

    public void setHostWindow(Window window) {
        this.hostWindow = window;
        if (headerController != null) headerController.setHostWindow(window);
    }

    public void setUserName(String name) {
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

    public void stopAllRefreshers() {
        programsController.stopProgramsRefresher();
        functionsController.stopFunctionsRefresher();
        usersController.stopUsersRefresher();
    }
}
