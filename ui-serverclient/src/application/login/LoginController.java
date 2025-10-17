package application.login;

import client.responses.LoginResponder;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(e -> doLogin());
    }

    private void doLogin() {
        String u = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        if (u.isEmpty()) {
            lblError.setText("username is required");
            return;
        }
        lblError.setText("");
        btnLogin.setDisable(true);

        // The responder throws IOException on failure; we let Task propagate it to onFailed.
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LoginResponder.execute(u); // throws on any HTTP/network/app error
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            btnLogin.setDisable(false);
            openMainAndClose();
        });

        task.setOnFailed(e -> {
            btnLogin.setDisable(false);
            Throwable ex = task.getException();
            lblError.setText(ex != null && ex.getMessage() != null ? ex.getMessage() : "Login error");
        });

        new Thread(task, "login-thread").start();
    }

    private void openMainAndClose() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/application/opening/opening_scene.fxml"));
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
