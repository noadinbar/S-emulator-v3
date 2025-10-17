package application.opening.users;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

public class UsersController {

    @FXML private BorderPane usersRoot;
    @FXML private Label titleLabel;
    @FXML private ListView<String> usersListView;
    @FXML private Button unselectButton;

    private final ObservableList<String> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        usersListView.setItems(users);
        // הכפתור מושבת כשאין בחירה
        if (unselectButton != null) {
            unselectButton.disableProperty().bind(
                    usersListView.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }

    // --- API חיצוני לקומפוננטה ---
    public void setUsers(List<String> newUsers) { users.setAll(newUsers); }

    public void addUser(String user) {
        if (!users.contains(user)) { users.add(user); }
    }

    public void removeUser(String user) { users.remove(user); }

    public String getSelectedUser() {
        return usersListView.getSelectionModel().getSelectedItem();
    }

    public void selectUser(String user) {
        usersListView.getSelectionModel().select(user);
        usersListView.scrollTo(user);
    }

    // --- Actions ---
    @FXML
    private void onUnselectUser() {
        usersListView.getSelectionModel().clearSelection();
    }
}
