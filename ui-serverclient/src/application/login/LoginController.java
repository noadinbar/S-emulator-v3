// ui-serverclient/src/application/login/LoginController.java
package application.login;

import client.responses.LoginResponder;
import users.LoginDTO;

import application.opening.OpeningSceneController;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    @FXML
    private void onLogin() {
        String user = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        if (user.isEmpty()) {
            lblError.setText("Please enter a user name");
            return;
        }

        Task<LoginDTO> task = new Task<>() {
            @Override protected LoginDTO call() throws Exception {
                return LoginResponder.execute(user);
            }
        };

        task.setOnSucceeded(e -> {
            LoginDTO dto = task.getValue();
            if (dto == null || !dto.isOk()) {
                lblError.setText(dto != null && dto.getError() != null ? dto.getError() : "Login failed");
                return;
            }
            openMainAndClose(dto.getUsername());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            lblError.setText(ex == null ? "Login failed" : ex.getMessage());
        });

        new Thread(task, "login-call").start();
    }

    /** Opens the main window and injects the logged-in user so it shows immediately. */
    private void openMainAndClose(String loggedUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/opening/opening_scene.fxml"));
            Parent root = loader.load();
            OpeningSceneController opening = loader.getController();
            // Your OpeningSceneController already exposes setUsers(List<String>)
            opening.setUsers(List.of(loggedUser));
            // And also setUserName(String) to update the header (stage 4)
            opening.setUserName(loggedUser);

            Stage stage = new Stage();
            stage.setTitle("S-emulator");
            stage.setScene(new Scene(root));
            stage.show();

            ((Stage) btnLogin.getScene().getWindow()).close();
        } catch (Exception ex) {
            lblError.setText("Failed to open main: " + ex.getMessage());
        }
    }
}
