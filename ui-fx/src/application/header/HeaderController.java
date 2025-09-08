package application.header;

import api.DisplayAPI;
import api.LoadAPI;
import exportToDTO.LoadAPIImpl;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class HeaderController {

    // עליון
    @FXML private Button btnLoad;
    @FXML private TextField txtPath;
    @FXML private ProgressBar progressBar;

    // תחתון (משמאל לימין)
    @FXML private ComboBox<String> cmbProgram;
    @FXML private Button btnCollapse;
    @FXML private TextField txtDegree;     // תצוגה בלבד
    @FXML private Button btnExpand;
    @FXML private ComboBox<String> cmbHighlight;

    // סטטוסי UI
    private final BooleanProperty busy   = new SimpleBooleanProperty(false); // בזמן טעינה
    private final BooleanProperty loaded = new SimpleBooleanProperty(false); // האם נטען תוכנית

    // callback שה-Controller הראשי ירשום כדי לקבל DisplayAPI טעון
    private final ObjectProperty<Consumer<DisplayAPI>> onLoaded = new SimpleObjectProperty<>();

    @FXML
    private void initialize() {
        txtPath.setEditable(false);
        txtDegree.setEditable(false);
        txtDegree.setFocusTraversable(false);
        txtDegree.setText("0 / 0");


        cmbProgram.disableProperty().bind(busy.or(loaded.not()));
        btnCollapse.disableProperty().bind(busy.or(loaded.not()));
        btnExpand.disableProperty().bind(busy.or(loaded.not()));
        cmbHighlight.disableProperty().bind(busy.or(loaded.not()));


        progressBar.setVisible(false);
    }


    public void setOnLoaded(Consumer<DisplayAPI> consumer) {
        onLoaded.set(consumer);
    }


    public void setDegree(int current, int max) {
        txtDegree.setText(current + " / " + max);
    }

    // === Handlers ===
    @FXML
    private void onLoadClicked() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose S-Emulator XML");

        // מאפשר לבחור רק קבצי XML (כולל .XML)
        FileChooser.ExtensionFilter xmlFilter =
                new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml", "*.XML");
        fc.getExtensionFilters().setAll(xmlFilter);
        fc.setSelectedExtensionFilter(xmlFilter);

        // ישירות מעל ה-Window של הכפתור (ללא משתנה owner)
        File f = fc.showOpenDialog(btnLoad.getScene().getWindow());
        if (f == null) return;

        // חגורת-וגומייה: אם איכשהו לא XML — עוצרים
        if (!f.getName().toLowerCase().endsWith(".xml")) {
            showError("Invalid file", "Please choose an .xml file.");
            return;
        }

        Path xml = f.toPath();
        txtPath.setText(xml.toString());

        // Task ברקע לטעינה
        Task<DisplayAPI> task = new Task<>() {
            @Override
            protected DisplayAPI call() throws Exception {
                updateProgress(0, 1);
                Thread.sleep(300);          // השהיה סימולטיבית קצרה להצגת progress
                updateProgress(0.3, 1);

                LoadAPI loader = new LoadAPIImpl();
                DisplayAPI display = loader.loadFromXml(xml);

                updateProgress(0.9, 1);
                Thread.sleep(300);
                updateProgress(1, 1);
                return display;
            }
        };


        busy.set(true);
        progressBar.setVisible(true);
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(ev -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            busy.set(false);
            loaded.set(true);

            DisplayAPI display = task.getValue();
            Consumer<DisplayAPI> apiConsumer = onLoaded.get();
            if (apiConsumer != null) apiConsumer.accept(display);
        });

        task.setOnFailed(ev -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            busy.set(false);

            Throwable ex = task.getException();
            showError("XML load failed",
                    (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error");
        });

        // הפעלה ברקע — כמו שסיכמנו ב-study
        new Thread(task, "xml-loader").start();
    }

    @FXML private void onProgramChanged()   { /* בהמשך */ }
    @FXML private void onCollapseClicked()  { /* בהמשך */ }
    @FXML private void onExpandClicked()    { /* בהמשך */ }
    @FXML private void onHighlightChanged() { /* בהמשך */ }

    // === Utils ===
    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
