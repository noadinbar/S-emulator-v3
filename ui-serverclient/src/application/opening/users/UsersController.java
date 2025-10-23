package application.opening.users;

import client.responses.authentication.UsersResponder;
import users.UserTableRowDTO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import utils.Constants;

public class UsersController {

    @FXML private BorderPane usersRoot;
    @FXML private Label titleLabel;

    @FXML private TableView<UserTableRowDTO> usersTable;
    @FXML private TableColumn<UserTableRowDTO, String>  colUserName;
    @FXML private TableColumn<UserTableRowDTO, Integer> colMainPrograms;
    @FXML private TableColumn<UserTableRowDTO, Integer> colFunctions;
    @FXML private TableColumn<UserTableRowDTO, Integer> colCreditsCurrent;
    @FXML private TableColumn<UserTableRowDTO, Integer> colCreditsUsed;
    @FXML private TableColumn<UserTableRowDTO, Integer> colRuns;

    @FXML private Button unselectButton;

    private final ObservableList<UserTableRowDTO> rows = FXCollections.observableArrayList();

    // Refresher infra (identical shape to Programs/Functions)
    private final AtomicBoolean shouldUpdate = new AtomicBoolean(false);
    private Timer timer;

    @FXML
    private void initialize() {
        // Bind table to model
        usersTable.setItems(rows);

        // Column → DTO getters
        colUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colMainPrograms.setCellValueFactory(new PropertyValueFactory<>("mainPrograms"));
        colFunctions.setCellValueFactory(new PropertyValueFactory<>("functions"));
        colCreditsCurrent.setCellValueFactory(new PropertyValueFactory<>("creditsCurrent"));
        colCreditsUsed.setCellValueFactory(new PropertyValueFactory<>("creditsUsed"));
        colRuns.setCellValueFactory(new PropertyValueFactory<>("runs"));

        // Disable button when nothing is selected
        if (unselectButton != null) {
            unselectButton.disableProperty().bind(
                    usersTable.getSelectionModel().selectedItemProperty().isNull()
            );
        }
        // שימי לב: כמו בשאר הקונטרולרים, לא מפעילים כאן את הרפרשר.
        // OpeningSceneController יקרא startUsersRefresher()/stopUsersRefresher().
    }

    // -----------------------------
    // Refresher control (async)
    // -----------------------------
    public void startUsersRefresher() {
        stopUsersRefresher();
        shouldUpdate.set(true);
        timer = new Timer(true); // daemon
        timer.scheduleAtFixedRate(
                new UsersRefresher(shouldUpdate, this::setUsersDto),
                0,
                Constants.REFRESH_RATE_MS
        );
    }

    public void stopUsersRefresher() {
        shouldUpdate.set(false);
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    // Optional: one-time prime to avoid the first 1s wait
    public void loadOnceAsync() {
        new Thread(() -> {
            try {
                var list = UsersResponder.list(); // runSync inside; not on FX thread
                setUsersDto(list);
            } catch (Exception ignore) { }
        }, "users-prime").start();
    }

    // -----------------------------
    // Public API
    // -----------------------------
    /** Set full DTO rows (used by refresher/prime). */
    public void setUsersDto(List<UserTableRowDTO> dtos) {
        rows.setAll(dtos);
    }

    /** Back-compat: accept only names and wrap to empty DTOs (useful if old code still calls setUsers). */
    public void setUsers(List<String> names) {
        rows.setAll(names.stream()
                .map(n -> new UserTableRowDTO(n, 0, 0, 0, 0, 0))
                .toList());
    }

    /** Return selected user's name (or null). */
    public String getSelectedUser() {
        var sel = usersTable.getSelectionModel().getSelectedItem();
        return sel == null ? null : sel.getName();
    }

    /** Select a row by user name. */
    public void selectUser(String user) {
        if (user == null) return;
        for (int i = 0; i < rows.size(); i++) {
            if (user.equalsIgnoreCase(rows.get(i).getName())) {
                usersTable.getSelectionModel().select(i);
                usersTable.scrollTo(i);
                break;
            }
        }
    }

    // --- Actions ---
    @FXML
    private void onUnselectUser() {
        usersTable.getSelectionModel().clearSelection();
    }
}
