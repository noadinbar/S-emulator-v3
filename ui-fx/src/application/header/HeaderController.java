package application.header;

import api.DisplayAPI;
import api.LoadAPI;
import display.Command2DTO;
import display.InstructionDTO;
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
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import types.LabelDTO;
import types.VarOptionsDTO;
import types.VarRefDTO;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

public class HeaderController {

    // upper
    @FXML private Button btnLoad;
    @FXML private TextField txtPath;
    @FXML private ProgressBar progressBar;
    private Path lastValidXmlPath;
    private File lastDir;
    // lower
    @FXML private ComboBox<String> cmbProgram;
    @FXML private Button btnCollapse;
    @FXML private TextField txtDegree;
    @FXML private Button btnExpand;
    @FXML private ComboBox<String> cmbHighlight;
    @FXML private ComboBox<String> cmbTheme;
    @FXML private Button helpButton;

    private Consumer<String> onThemeChanged;
    // UI state properties
    private final BooleanProperty busy   = new SimpleBooleanProperty(false); // בזמן טעינה
    private final BooleanProperty loaded = new SimpleBooleanProperty(false); // האם נטען תוכנית
    private final IntegerProperty currentDegree = new SimpleIntegerProperty(0);
    private final IntegerProperty maxDegree     = new SimpleIntegerProperty(0);
    private final ObjectProperty<Runnable> onExpand   = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onCollapse = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<DisplayAPI>> onLoaded = new SimpleObjectProperty<>();


    private Consumer<String> onHighlightChangedCb;


    @FXML
    private void initialize() {
        txtPath.setEditable(false);
        txtDegree.setEditable(false);
        txtDegree.setFocusTraversable(false);

        txtDegree.disableProperty().bind(busy.or(loaded.not()));
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

        if (cmbTheme != null) {
            cmbTheme.getItems().setAll("Default", "Rose", "Sky");
            cmbTheme.getSelectionModel().select("Default");
            cmbTheme.getSelectionModel().selectedItemProperty().addListener((obs, o, v) -> {
                if (onThemeChanged == null) return;
                String theme = "Rose".equals(v) ? "theme-rose" : "Sky".equals(v)  ? "theme-sky"  : "theme-default"; onThemeChanged.accept(theme);});
        }

        if (cmbHighlight != null) {
            cmbHighlight.valueProperty().addListener((obs, oldV, newV) -> {
                if (onHighlightChangedCb != null) {
                    onHighlightChangedCb.accept(newV);
                }
            });
        }
    }

    public String getSelectedHighlight() {
        String v = (cmbHighlight != null) ? cmbHighlight.getValue() : null;
        return (v == null || "NONE".equals(v)) ? null : v;
    }



    public void setOnLoaded(Consumer<DisplayAPI> consumer) {
        onLoaded.set(consumer);
    }
    public void setOnThemeChanged(Consumer<String> cb) { this.onThemeChanged = cb; }



    // === Handlers ===
    @FXML
    private void onLoadClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose S-Emulator XML");


        FileChooser.ExtensionFilter xmlFilter =
                new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml", "*.XML");
        chooser.getExtensionFilters().setAll(xmlFilter);
        chooser.setSelectedExtensionFilter(xmlFilter);

        try {
            if (lastDir != null && lastDir.isDirectory()) {
                chooser.setInitialDirectory(lastDir);
            } else if (lastValidXmlPath!= null && lastValidXmlPath.getParent() != null) {
                File parent = lastValidXmlPath.getParent().toFile();
                if (parent.isDirectory()) chooser.setInitialDirectory(parent);
            } else {
                File home = new File(System.getProperty("user.home"));
                if (home.isDirectory()) chooser.setInitialDirectory(home);
            }
        } catch (SecurityException ignore) {

        }


        File file = chooser.showOpenDialog(btnLoad.getScene().getWindow());
        if (file == null) return;

        lastDir = file.getParentFile();


        if (!file.getName().toLowerCase().endsWith(".xml")) {
            showError("Invalid file", "Please choose an .xml file.");
            return;
        }
        final Path chosenXml = file.toPath();



        Task<DisplayAPI> task = new Task<>() {
            @Override
            protected DisplayAPI call() throws Exception {
                updateProgress(0, 1);
                Thread.sleep(300);
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


        new Thread(task, "xml-loader").start();
    }
    @FXML private void onExpandClicked()   { var r = onExpand.get();   if (r != null) r.run(); }
    @FXML private void onCollapseClicked() { var r = onCollapse.get(); if (r != null) r.run(); }

    public void setOnExpand(Runnable r)   { onExpand.set(r); }
    public void setOnCollapse(Runnable r) { onCollapse.set(r); }

    public void setCurrentDegree(int current) { currentDegree.set(current); }
    public void setMaxDegree(int max)         { maxDegree.set(max); }
    public int getCurrentDegree() { return currentDegree.get(); }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to use this screen");

        TextArea area = new TextArea("""
                Open the app and click Load File to choose an .xml program.
                (Optional) Pick a Theme in the header.
                Press Start to show the Inputs panel, edit the values, then press Run to execute.
                The program view (left) shows the Instructions table; the right side shows Variables/Inputs; results appear in Outputs.
                Use Expand/Collapse to change the current Degree.
                Every run is recorded in History (bottom-right). To repeat a past run, select a row in History and click RERUN—inputs and degree are restored automatically.
                Tip: you must press Start before Run the first time after loading a file.
                """);
        area.setEditable(false);
        area.setWrapText(true);
        alert.getDialogPane().setContent(area);

        Window owner = (helpButton != null && helpButton.getScene() != null)
                ? helpButton.getScene().getWindow() : null;
        if (owner != null) alert.initOwner(owner);

        alert.showAndWait();
    }


    @FXML private void onProgramChanged()   { /* בהמשך */ }


    @FXML private void onHighlightChanged() {
        if (onHighlightChangedCb != null && cmbHighlight != null) {
        onHighlightChangedCb.accept(cmbHighlight.getValue());
    }
    }

    public void setOnHighlightChanged(Consumer<String> cb) { this.onHighlightChangedCb= cb; }

    public void populateHighlight(List<InstructionDTO> rows) {
        populateHighlight(rows, false); // ברירת מחדל: לא לאפס
    }

    public void populateHighlight(List<InstructionDTO> rows, boolean resetToNone) {
        if (cmbHighlight == null) return;

        String prev = cmbHighlight.getValue();
        if (resetToNone) prev = null; // ← מבטל שימור בחירה קודמת

        boolean hasY = false;
        boolean jumpToExit = false;
        SortedSet<Integer> xs = new TreeSet<>();
        SortedSet<Integer> zs = new TreeSet<>();
        SortedSet<Integer> ls = new TreeSet<>();

        if (rows != null) {
            for (InstructionDTO ins : rows) {
                LabelDTO lbl = ins.getLabel();
                if (lbl != null && !lbl.isExit()) {
                    String name = lbl.getName();
                    if (name != null && name.startsWith("L")) {
                        try { ls.add(Integer.parseInt(name.substring(1))); } catch (Exception ignore) {}
                    }
                }
                var body = ins.getBody();
                if (body == null) continue;

                LabelDTO jt = body.getJumpTo();
                if (jt != null && jt.isExit()) jumpToExit = true;

                for (VarRefDTO ref : new VarRefDTO[]{
                        body.getVariable(), body.getDest(), body.getSource(), body.getCompare(), body.getCompareWith()
                }) {
                    if (ref == null) continue;
                    switch (ref.getVariable()) {
                        case y -> hasY = true;
                        case x -> xs.add(ref.getIndex());
                        case z -> zs.add(ref.getIndex());
                    }
                }
            }
        }

        List<String> list = new ArrayList<>();
        list.add("NONE");                 // תמיד ראשון
        if (hasY) list.add("y");
        for (int i : xs) list.add("x" + i);
        for (int i : zs) list.add("z" + i);
        for (int i : ls) list.add("L" + i);
        if (jumpToExit) list.add("EXIT");

        cmbHighlight.getItems().setAll(list);

        if (prev != null && list.contains(prev)) {
            cmbHighlight.setValue(prev);
        } else {
            cmbHighlight.setValue("NONE"); // ← אפס לברירת המחדל
        }
    }

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
