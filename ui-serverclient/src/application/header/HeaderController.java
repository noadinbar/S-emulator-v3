package application.header;

import api.DisplayAPI;
import api.LoadAPI;
import client.responses.LoadFileResponder;
import display.*;
import exportToDTO.LoadAPIImpl;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;
import types.LabelDTO;
import types.VarRefDTO;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderController {
    @FXML private Label Title;
    @FXML private Button btnLoad;
    @FXML private TextField txtPath;
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<String> cmbProgramFunction;
    @FXML private Button btnCollapse;
    @FXML private TextField txtDegree;
    @FXML private TextField txtMaxDegree;
    @FXML private Button btnExpand;
    @FXML private ComboBox<String> cmbHighlight;
    @FXML private ComboBox<String> cmbSkin;
    @FXML private ComboBox<String> cmbAnimations;
    @FXML private Button helpButton;

    private Path lastValidXmlPath;
    private File lastDir;
    private final BooleanProperty busy   = new SimpleBooleanProperty(false);
    private final BooleanProperty loaded = new SimpleBooleanProperty(false);
    private final IntegerProperty currentDegree = new SimpleIntegerProperty(0);
    private final IntegerProperty maxDegree     = new SimpleIntegerProperty(0);
    private final ObjectProperty<Runnable> onExpand   = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onCollapse = new SimpleObjectProperty<>();
    private final ObjectProperty<Consumer<DisplayDTO>> onLoaded = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onApplyDegree = new SimpleObjectProperty<>();
    private Consumer<Boolean> onAnimationsChanged;
    private TextFormatter<Integer> degreeFormatter;

    private Consumer<String> onSkinChanged;
    private Consumer<String> onHighlightChangedCb;
    private Consumer<String> onProgramSelectedCb;
    private static final Pattern X_IN_ARGS = Pattern.compile("\\bx(\\d+)\\b");
    private static final Pattern Z_IN_ARGS = Pattern.compile("\\bz(\\d+)\\b");

    @FXML
    private void initialize() {
        txtPath.setEditable(false);
        txtDegree.disableProperty().bind(busy.or(loaded.not()));
        cmbProgramFunction.disableProperty().bind(busy.or(loaded.not()));
        cmbHighlight.disableProperty().bind(busy.or(loaded.not()));

        UnaryOperator<TextFormatter.Change> filter = c -> {
            String nt = c.getControlNewText();
            if (nt.isEmpty()) return c;
            return nt.matches("\\d{1,10}") ? c : null;
        };
        degreeFormatter = new TextFormatter<>(new IntegerStringConverter(), 0, filter);
        txtDegree.setTextFormatter(degreeFormatter);

        degreeFormatter.valueProperty().bindBidirectional(currentDegree.asObject());

        txtDegree.setOnAction(e -> onDegreeEnter());

        if (txtMaxDegree != null) {
            txtMaxDegree.textProperty().bind(maxDegree.asString());
            txtMaxDegree.disableProperty().bind(busy.or(loaded.not()));
        }

        maxDegree.addListener((obs, ov, nv) -> {
            int max = (nv == null ? 0 : nv.intValue());
            if (currentDegree.get() > max) currentDegree.set(max);
            if (currentDegree.get() < 0)   currentDegree.set(0);
        });

        degreeFormatter.valueProperty().addListener((o, ov, nv) -> {
            if (nv == null) currentDegree.set(0);
        });

        btnCollapse.disableProperty().bind(
                busy.or(loaded.not()).or(currentDegree.lessThanOrEqualTo(0))
        );
        btnExpand.disableProperty().bind(
                busy.or(loaded.not()).or(currentDegree.greaterThanOrEqualTo(maxDegree))
        );

        progressBar.setVisible(false);

        // skin combobox
        if (cmbSkin != null) {
            cmbSkin.getItems().setAll("Default", "Rose", "Sky");
            cmbSkin.getSelectionModel().select("Default");
            cmbSkin.getSelectionModel().selectedItemProperty().addListener((obs, o, v) -> {
                if (onSkinChanged == null) return;
                String skin = "Rose".equals(v) ? "skin-rose"
                        : "Sky".equals(v)     ? "skin-sky"
                        : "skin-default";
                onSkinChanged.accept(skin);
            });
        }

        // Highlight combobox
        if (cmbHighlight != null) {
            cmbHighlight.valueProperty().addListener((obs, oldV, newV) -> {
                if (onHighlightChangedCb != null) {
                    onHighlightChangedCb.accept(newV);
                }
            });
        }

        if (cmbAnimations != null) {
            cmbAnimations.getItems().setAll("Animation off", "Animation on");
            cmbAnimations.getSelectionModel().select("Animation off"); //
            cmbAnimations.getSelectionModel().selectedItemProperty().addListener((obs, o, v) -> {
                boolean enabled = !"Animation off".equalsIgnoreCase(String.valueOf(v));
                if (onAnimationsChanged != null) onAnimationsChanged.accept(enabled);
            });
        }
    }

    public String getSelectedHighlight() {
        String v = (cmbHighlight != null) ? cmbHighlight.getValue() : null;
        return (v == null || "NONE".equals(v)) ? null : v;
    }

    public void setOnLoaded(Consumer<DisplayDTO> c) { onLoaded.set(c); }
    public void setOnSkinChanged(Consumer<String> cb) { this.onSkinChanged = cb; }
    public void setOnProgramSelected(Consumer<String> cb) { this.onProgramSelectedCb = cb; }
    public void setOnAnimationsChanged(Consumer<Boolean> cb) { this.onAnimationsChanged = cb; }
    public void setOnApplyDegree(Runnable r) { onApplyDegree.set(r); }
    public void setOnExpand(Runnable r)   { onExpand.set(r); }
    public void setOnCollapse(Runnable r) { onCollapse.set(r); }
    public void setCurrentDegree(int current) {
        currentDegree.set(current);
        syncDegreeField();
    }
    public void setMaxDegree(int max) { maxDegree.set(max); }
    public int getCurrentDegree() { return currentDegree.get(); }
    public String getSelectedProgramFunction() {
        if (cmbProgramFunction == null) return null;
        String selected = cmbProgramFunction.getValue();
        if (selected == null && !cmbProgramFunction.getItems().isEmpty()) {
            cmbProgramFunction.getSelectionModel().selectFirst();
            selected = cmbProgramFunction.getValue();
        }
        return selected;
    }


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
        Task<DisplayDTO> task = new Task<>() {
            @Override
            protected DisplayDTO call() throws Exception {
                updateProgress(0,1);
                Thread.sleep(300);
                updateProgress(0.3,1);
                DisplayDTO dto = LoadFileResponder.execute(chosenXml);
                updateProgress(0.9,1);
                Thread.sleep(300);
                updateProgress(1,1);
                return dto;
            }
        };

        task.setOnSucceeded(ev -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            busy.set(false);
            loaded.set(true);

            lastValidXmlPath = chosenXml;
            txtPath.setText(chosenXml.toString());

            DisplayDTO dto = task.getValue();
            Consumer<DisplayDTO> consumer = onLoaded.get();
            if (consumer != null) consumer.accept(dto);
        });

        task.setOnFailed(ev -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            busy.set(false);
            Throwable ex = task.getException();
            showError("XML load failed",
                    (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error");
        });
        new Thread(task, "load-xml").start();
    }
    @FXML private void onExpandClicked() {
        int next = Math.min(currentDegree.get() + 1, maxDegree.get());
        if (next != currentDegree.get()) currentDegree.set(next);
        syncDegreeField();
        Runnable r = onExpand.get();      if (r != null) r.run();
        Runnable a = onApplyDegree.get(); if (a != null) a.run();
    }

    @FXML
    private void onCollapseClicked() {
        int next = Math.max(currentDegree.get() - 1, 0);
        if (next != currentDegree.get()) currentDegree.set(next);
        syncDegreeField();
        Runnable r = onCollapse.get();    if (r != null) r.run();
        Runnable a = onApplyDegree.get(); if (a != null) a.run();
    }

    @FXML
    private void onDegreeEnter() {
        String raw = txtDegree.getText();
        int max = maxDegree.get();

        if (raw == null || raw.isBlank() || !raw.matches("\\d+")) {
            showError("Invalid degree", "Please enter an integer between 0 and " + max + ".");
            Platform.runLater(() -> { txtDegree.requestFocus(); txtDegree.selectAll(); });
            return;
        }

        int d;
        try {
            d = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            showError("Invalid degree", "Please enter an integer between 0 and " + max + ".");
            Platform.runLater(() -> { txtDegree.requestFocus(); txtDegree.selectAll(); });
            return;
        }

        if (d < 0 || d > max) {
            showError("Out of range", "Degree must be between 0 and " + max + ". Setting to the nearest allowed value.");
            d = Math.max(0, Math.min(d, max));
        }

        if (d != currentDegree.get()) currentDegree.set(d);

        Runnable apply = onApplyDegree.get();
        if (apply != null) apply.run();
    }

    private void syncDegreeField() {
        int v = currentDegree.get();
        if (degreeFormatter != null && !Objects.equals(degreeFormatter.getValue(), v)) {
            degreeFormatter.setValue(v);
        } else if (txtDegree != null && (txtDegree.getText() == null || !txtDegree.getText().equals(Integer.toString(v)))) {
            txtDegree.setText(Integer.toString(v));
        }
    }

    @FXML
    private void onHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("How to use");

        TextArea area = new TextArea("""
                To load a file-> click Load File, navigate through your file system and choose the file you want to load.
                **YOU CAN ONLY CHOOSE .XML FILES**
                
                Press Start to show the Inputs panel, edit the values, then choose between Run/Debug mode, and then press Execute to run with the chosen mode.
                The program view (left) shows the Instructions table, when you click on a row, you can see its ancestry in the bottom table.
                You can choose between running the program or each function alone at the combobox on the left.
                The right side shows Variables/Inputs; results appear in Outputs.
                Use Expand/Collapse to change the current Degree or you can directly write the wanted degree and press ENTER. 
                Every run/debug is recorded in History (bottom-right).
                To repeat a past run, select a row in History and click Rerun—inputs and degree are restored automatically.
                To see the values of all the variables from past runs/debugs select a row in History and click Show.
                
                TIP: - You must press Start before executing. 
                     - Inputs can be edited ONLY after clicking Start/Rerun.
                      
                BONUSES: - You can choose a skin for the system as you like.
                """);
        area.setEditable(false);
        area.setWrapText(true);
        alert.getDialogPane().setContent(area);

        Window owner = (helpButton != null && helpButton.getScene() != null)
                ? helpButton.getScene().getWindow() : null;
        if (owner != null) alert.initOwner(owner);

        alert.showAndWait();
    }

    @FXML private void onProgramChanged()   {
        if (onProgramSelectedCb != null && cmbProgramFunction != null) {
            onProgramSelectedCb.accept(cmbProgramFunction.getValue());
        }
    }

    @FXML private void onHighlightChanged() {
        if (onHighlightChangedCb != null && cmbHighlight != null) {
        onHighlightChangedCb.accept(cmbHighlight.getValue());
    }
    }

    public void setOnHighlightChanged(Consumer<String> cb) { this.onHighlightChangedCb= cb; }

    public void populateHighlight(List<InstructionDTO> rows) {
        populateHighlight(rows, false);
    }

    public void populateHighlight(List<InstructionDTO> rows, boolean resetToNone) {
        if (cmbHighlight == null) return;

        String prev = cmbHighlight.getValue();
        if (resetToNone) prev = null;

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
                InstructionBodyDTO body = ins.getBody();
                if (body == null) continue;
                if (body.getOp() == InstrOpDTO.QUOTE || body.getOp() == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    parseVarsFromArgs(body.getFunctionArgs(), xs, zs);
                }


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
        list.add("Highlight selection");
        if (hasY) list.add("y");
        for (int i : xs) list.add("x" + i);
        for (int i : zs) list.add("z" + i);
        for (int i : ls) list.add("L" + i);
        if (jumpToExit) list.add("EXIT");

        cmbHighlight.getItems().setAll(list);

        if (prev != null && list.contains(prev)) {
            cmbHighlight.setValue(prev);
        } else {
            cmbHighlight.setValue("Highlight selection");
        }
    }

    private static void parseVarsFromArgs(String text, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (text == null || text.isBlank()) return;
        Matcher mx = X_IN_ARGS.matcher(text);
        while (mx.find()) { try { xs.add(Integer.parseInt(mx.group(1))); } catch (Exception ignore) {} }
        Matcher mz = Z_IN_ARGS.matcher(text);
        while (mz.find()) { try { zs.add(Integer.parseInt(mz.group(1))); } catch (Exception ignore) {} }
    }

    public void populateProgramFunction(DisplayDTO dto) {
        populateProgramFunction(dto, /*resetToProgram*/ false);
    }

    public void populateProgramFunction(DisplayDTO dto, boolean resetToProgram) {
        if (cmbProgramFunction == null) return;

        String prev = resetToProgram ? null : cmbProgramFunction.getValue();
        List<String> items = new ArrayList<>();

        if (dto != null) {
            // Program row
            String programName = dto.getProgramName();
            items.add("PROGRAM: " + (programName != null ? programName : "Unnamed"));

            // Functions rows
            if (dto.getFunctions() != null) {
                for (FunctionDTO f : dto.getFunctions()) {
                    String user = (f.getUserString() != null && !f.getUserString().isBlank())
                            ? f.getUserString()
                            : f.getName();
                    if (user != null && !user.isBlank()) {
                        items.add("FUNCTION: " + user);
                    }
                }
            }
        }

        cmbProgramFunction.getItems().setAll(items);
        if (prev != null && items.contains(prev)) {
            cmbProgramFunction.setValue(prev);
        } else if (!items.isEmpty()) {
            cmbProgramFunction.getSelectionModel().selectFirst();
        }
    }


    // === Utils ===
    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            TextArea area = new TextArea(msg);
            area.setEditable(false);
            area.setWrapText(true);
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        });
    }

    public boolean isAnimationsOn() {
        String v = (cmbAnimations != null) ? cmbAnimations.getValue() : "Animation on";
        return !"Animation off".equalsIgnoreCase(String.valueOf(v));
    }
}
