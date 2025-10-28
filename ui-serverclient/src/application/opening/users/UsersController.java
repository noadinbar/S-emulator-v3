package application.opening.users;

import client.responses.authentication.UsersResponder;
import javafx.scene.layout.BorderPane;
import users.UserTableRowDTO;
import utils.Constants;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class UsersController {

    @FXML private TableView<UserTableRowDTO> usersTable;
    @FXML private TableColumn<UserTableRowDTO, String>  colUserName;
    @FXML private TableColumn<UserTableRowDTO, Integer> colMainPrograms;
    @FXML private TableColumn<UserTableRowDTO, Integer> colFunctions;
    @FXML private TableColumn<UserTableRowDTO, Integer> colCreditsCurrent;
    @FXML private TableColumn<UserTableRowDTO, Integer> colCreditsUsed;
    @FXML private TableColumn<UserTableRowDTO, Integer> colRuns;

    @FXML private Button unselectButton;
    @FXML private Label titleLabel;      // stays even if not used right now
    @FXML private BorderPane usersRoot; // stays

    private final AtomicBoolean shouldUpdate = new AtomicBoolean(true);
    private Timer timer;
    private UsersRefresher refresher;
    private String selectedUserName;
    private Consumer<String> onUserSelectionChanged;
    private boolean suppressSelectionEvents = false;

    @FXML
    private void initialize() {
        // bind table columns to DTO getters
        colUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMainPrograms.setCellValueFactory(new PropertyValueFactory<>("mainPrograms"));
        colFunctions.setCellValueFactory(new PropertyValueFactory<>("functions"));
        colCreditsCurrent.setCellValueFactory(new PropertyValueFactory<>("creditsCurrent"));
        colCreditsUsed.setCellValueFactory(new PropertyValueFactory<>("creditsUsed"));
        colRuns.setCellValueFactory(new PropertyValueFactory<>("runs"));

        // disable the "unselect" button if nothing is selected
        unselectButton.disableProperty().bind(
                usersTable.getSelectionModel().selectedItemProperty().isNull()
        );

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedUserName = (newVal != null ? newVal.getName() : null);
            if (suppressSelectionEvents) {
                return;
            }
            if (onUserSelectionChanged != null) {
                if (newVal == null) {
                    // no selection -> show current user's own history
                    onUserSelectionChanged.accept(null);
                } else {
                    // specific user selected -> show that user's history
                    onUserSelectionChanged.accept(newVal.getName());
                }
            }
        });
    }

    @FXML
    private void onUnselectUser() {
        usersTable.getSelectionModel().clearSelection();
        if (onUserSelectionChanged != null) {
            // null means "show the logged-in user's own history"
            onUserSelectionChanged.accept(null);
        }
    }

    public void startUsersRefresher() {
        if (timer != null) {
            return;
        }
        refresher = new UsersRefresher(shouldUpdate, this::applyRows);
        timer = new Timer(true);
        timer.schedule(refresher, Constants.REFRESH_RATE_MS, Constants.REFRESH_RATE_MS);
    }

    public void stopUsersRefresher() {
        shouldUpdate.set(false);
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    // one-time prime, same idea כמו loadOnceAsync ב-ProgramsController
    public void loadOnceAsync() {
        Thread t = new Thread(() -> {
            try {
                List<UserTableRowDTO> list = UsersResponder.list();
                Platform.runLater(() -> applyRows(list));
            } catch (Exception ignore) { }
        }, "users-prime");
        t.setDaemon(true);
        t.start();
    }

    private void applyRows(List<UserTableRowDTO> rows) {
        String keep = selectedUserName;
        // block selection listener while we refresh table content and restore selection
        suppressSelectionEvents = true;
        usersTable.getItems().setAll(rows);
        if (keep != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (keep.equalsIgnoreCase(rows.get(i).getName())) {
                    usersTable.getSelectionModel().select(i);
                    usersTable.scrollTo(i);
                    suppressSelectionEvents = false;
                    return;
                }
            }
        }
        // previous selected user no longer exists
        usersTable.getSelectionModel().clearSelection();
        suppressSelectionEvents = false;
    }

    public void setOnUserSelectionChanged(Consumer<String> consumer) {
        this.onUserSelectionChanged = consumer;
    }

    public String getSelectedUser() {
        UserTableRowDTO sel = usersTable.getSelectionModel().getSelectedItem();
        return sel == null ? null : sel.getName();
    }

    public void selectUser(String user) {
        if (user == null) {
            return;
        }
        List<UserTableRowDTO> current = usersTable.getItems();
        for (int i = 0; i < current.size(); i++) {
            if (user.equalsIgnoreCase(current.get(i).getName())) {
                usersTable.getSelectionModel().select(i);
                usersTable.scrollTo(i);
                break;
            }
        }
    }

    public void setUsers(List<String> names) {
        if (names == null) {
            return;
        }
        List<UserTableRowDTO> dtoList = names.stream()
                .map(n -> new UserTableRowDTO(n, 0, 0, 0, 0, 0))
                .toList();
        applyRows(dtoList);
    }
}
