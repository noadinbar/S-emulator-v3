package application.header;

import api.DisplayAPI;
import api.LoadAPI;
import exportToDTO.LoadAPIImpl;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
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
    private final IntegerProperty currentDegree = new SimpleIntegerProperty(0);
    private final IntegerProperty maxDegree     = new SimpleIntegerProperty(0);
    private final ObjectProperty<Runnable> onExpand   = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onCollapse = new SimpleObjectProperty<>();

    // callback שה-Controller הראשי ירשום כדי לקבל DisplayAPI טעון
    private final ObjectProperty<Consumer<DisplayAPI>> onLoaded = new SimpleObjectProperty<>();
    // בראש המחלקה (שדות) — הוסיפי:
    private Path lastValidXmlPath;
    // בראש המחלקה:
    private File lastDir; // התיקייה האחרונה שנבחר ממנה קובץ


    @FXML
    private void initialize() {
        txtPath.setEditable(false);
        txtDegree.setEditable(false);
        txtDegree.setFocusTraversable(false);
        txtDegree.setText("0 / 0");


        cmbProgram.disableProperty().bind(busy.or(loaded.not()));
        cmbHighlight.disableProperty().bind(busy.or(loaded.not()));

        txtDegree.textProperty().bind(
                Bindings.createStringBinding(
                        () -> currentDegree.get() + " / " + maxDegree.get(),
                        currentDegree, maxDegree
                )
        );
        btnCollapse.disableProperty().bind(
                busy.or(loaded.not()).or(currentDegree.lessThanOrEqualTo(0))
        );
        btnExpand.disableProperty().bind(
                busy.or(loaded.not()).or(currentDegree.greaterThanOrEqualTo(maxDegree))
        );

        progressBar.setVisible(false);


    }


    public void setOnLoaded(Consumer<DisplayAPI> consumer) {
        onLoaded.set(consumer);
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

        try {
            if (lastDir != null && lastDir.isDirectory()) {
                fc.setInitialDirectory(lastDir);
            } else if (lastValidXmlPath!= null && lastValidXmlPath.getParent() != null) {
                File parent = lastValidXmlPath.getParent().toFile();
                if (parent.isDirectory()) fc.setInitialDirectory(parent);
            } else {
                File home = new File(System.getProperty("user.home"));
                if (home.isDirectory()) fc.setInitialDirectory(home);
            }
        } catch (SecurityException ignore) {
            // במידה וה־OS/הרשאות חוסמות גישה — פשוט מתעלמים וניתן ל־FileChooser לבחור ברירת מחדל
        }

        // ישירות מעל ה-Window של הכפתור (ללא משתנה owner)
        File f = fc.showOpenDialog(btnLoad.getScene().getWindow());
        if (f == null) return;

        lastDir = f.getParentFile();

        // חגורת-וגומייה: אם איכשהו לא XML — עוצרים
        if (!f.getName().toLowerCase().endsWith(".xml")) {
            showError("Invalid file", "Please choose an .xml file.");
            return;
        }
        final Path chosenXml = f.toPath();


        // Task ברקע לטעינה
        Task<DisplayAPI> task = new Task<>() {
            @Override
            protected DisplayAPI call() throws Exception {
                updateProgress(0, 1);
                Thread.sleep(300);          // השהיה סימולטיבית קצרה להצגת progress
                updateProgress(0.3, 1);

                LoadAPI loader = new LoadAPIImpl();
                DisplayAPI display = loader.loadFromXml(chosenXml);

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

            lastValidXmlPath = chosenXml;
            txtPath.setText(chosenXml.toString());

            DisplayAPI display = task.getValue();
            Consumer<DisplayAPI> apiConsumer = onLoaded.get();
            if (apiConsumer != null) apiConsumer.accept(display);
        });

        task.setOnFailed(ev -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            busy.set(false);

            if (lastValidXmlPath != null) {
                txtPath.setText(lastValidXmlPath.toString());
            } else {
                txtPath.clear();
            }

            Throwable ex = task.getException();
            showError("XML load failed",
                    (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error");
        });

        // הפעלה ברקע — כמו שסיכמנו ב-study
        new Thread(task, "xml-loader").start();
    }
    @FXML private void onExpandClicked()   { var r = onExpand.get();   if (r != null) r.run(); }
    @FXML private void onCollapseClicked() { var r = onCollapse.get(); if (r != null) r.run(); }

    public void setOnExpand(Runnable r)   { onExpand.set(r); }
    public void setOnCollapse(Runnable r) { onCollapse.set(r); }

    public void setCurrentDegree(int current) { currentDegree.set(current); }
    public void setMaxDegree(int max)         { maxDegree.set(max); }
    public int getCurrentDegree() { return currentDegree.get(); }

    // אופציונלי לשמירה על תאימות:
   // public void setDegree(int current, int max) { currentDegree.set(current); maxDegree.set(max); }

    @FXML private void onProgramChanged()   { /* בהמשך */ }


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
