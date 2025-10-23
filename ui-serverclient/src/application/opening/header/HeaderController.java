package application.opening.header;

import java.io.File;
import java.util.function.Consumer;

import client.responses.info.StatusResponder;
import client.responses.runtime.LoadFileResponder;
import com.google.gson.JsonObject;
import display.UploadResultDTO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class HeaderController {
    @FXML private VBox headerOpeningRoot;
    @FXML private Label userNameLabel;
    @FXML private Label dashboardTitle;
    @FXML private Label availableCreditsField;

    // --- bottom row ---
    @FXML private Button loadFileButton;
    @FXML private TextField loadedFilePathField;
    @FXML private Button chargeCreditsButton;
    @FXML private TextField chargeAmountField;
    private Window hostWindow;

    private Consumer<File> onLocalFileChosen;
    private Consumer<Integer> onChargeCredits;

    @FXML
    private void initialize() {
        if (dashboardTitle != null && dashboardTitle.getText() == null) {
            dashboardTitle.setText("S-Emulator - Dashboard");
        }
        if (availableCreditsField != null) {
            availableCreditsField.setFocusTraversable(false);
        }
        if (loadedFilePathField != null) {
            loadedFilePathField.setEditable(false);
        }
    }

    @FXML
    private void onLoadFile() {
        Window w = (hostWindow != null) ? hostWindow :
                (headerOpeningRoot != null && headerOpeningRoot.getScene() != null
                        ? headerOpeningRoot.getScene().getWindow() : null);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select XML file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = chooser.showOpenDialog(w);
        if (file == null) return;

        if (loadedFilePathField != null) {
            loadedFilePathField.setText(file.getAbsolutePath());
        }

        // ---- Upload-only Task (no DisplayAPI/DisplayDTO) ----
        loadFileButton.setDisable(true);

        final File chosen = file;
        Task<UploadResultDTO> task = new Task<>() {
            @Override
            protected UploadResultDTO call() throws Exception {
                return LoadFileResponder.execute(chosen.toPath());
            }
        };

        task.setOnSucceeded(ev -> {
            loadFileButton.setDisable(false);
            UploadResultDTO res = task.getValue();
            if (res == null) {
                showError("Upload failed", "Server returned no result.");
                return;
            }
            if (!res.isOk()) {
                String msg = (res.getError() != null) ? res.getError() : "Upload not ok";
                showError("Upload failed", msg);
                return;
            }
        });

        task.setOnFailed(ev -> {
            loadFileButton.setDisable(false);
            Throwable ex = task.getException();
            showError("Upload failed",
                    (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error");
        });

        Thread t = new Thread(task, "upload-xml");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onChargeCredits() {
        if (onChargeCredits == null) return;
        String text = (chargeAmountField != null) ? chargeAmountField.getText() : null;
        if (text == null || text.isBlank()) return;
        try {
            int amount = Integer.parseInt(text.trim());
            if (amount > 0) onChargeCredits.accept(amount);
        } catch (NumberFormatException ignore) {
        }
    }

    public void setHostWindow(Window window) { this.hostWindow = window; }
    public void setUserName(String name) {
        if (userNameLabel != null) userNameLabel.setText(name);
    }

    public void setAvailableCredits(int credits) {
        if (availableCreditsField != null) availableCreditsField.setText(Integer.toString(credits));
    }

    public void setOnLocalFileChosen(Consumer<File> cb) { this.onLocalFileChosen = cb; }
    public void setOnChargeCredits(Consumer<Integer> cb) { this.onChargeCredits = cb; }
    public void setChargeAmountField(String amount) { chargeAmountField.setText(amount); }


    public void refreshStatus() {
        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                // GET /api/status (Gson JSON)
                return StatusResponder.get();
            }
        };

        task.setOnSucceeded(ev -> {
            JsonObject js = task.getValue();
            if (js == null) {
                return;
            }

            // username (may be null before login)
            if (js.has("username") && !js.get("username").isJsonNull()) {
                String u = js.get("username").getAsString();
                if (u != null && !u.isBlank()) {
                    // Assumes these setters already exist in your HeaderController
                    setUserName(u);
                }
            }

            // credits (show only when present)
            if (js.has("creditsCurrent") && !js.get("creditsCurrent").isJsonNull()) {
                int credits = js.get("creditsCurrent").getAsInt();
                setAvailableCredits(credits);
            }
        });

        task.setOnFailed(ev -> {
            // Optional: log/ignore – header stays as-is on failure
        });

        new Thread(task, "header-refresh-status").start();
    }


    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        TextArea area = new TextArea(msg);
        area.setEditable(false);
        area.setWrapText(true);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
