package application.opening;

import execution.RunHistoryEntryDTO;

import java.io.File;
import java.util.List;
import javafx.fxml.FXML;
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

    // --- Hookים נקודתיים: כאן תוסיפי בהמשך ClientService להעלאה/קרדיטים ---
    private void handleLocalFileChosen(File file) {
        // TODO: upload לשרת דרך ClientService → ואז refresh ל-Programs/Functions/History
        // דוגמה בהמשך: client.uploadXml(file); refreshLists();
    }

    private void handleChargeCredits(int amount) {
        // TODO: לקרוא לשרת (client.chargeCredits(amount)) ואז לרענן available credits
    }

    public void stopAllRefreshers() {
        programsController.stopProgramsRefresher();
        functionsController.stopFunctionsRefresher();
        usersController.stopUsersRefresher();
    }
}
