package application.execution.header;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import client.responses.info.StatusResponder;
import com.google.gson.JsonObject;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.scene.control.TextFormatter;
import types.LabelDTO;
import types.VarRefDTO;

public class HeaderController {
    @FXML private VBox   executionHeaderRoot;
    @FXML private Label  userNameLabel;
    @FXML private Label  titleLabel;
    @FXML private Label runTargetLabel;
    @FXML private Label availableCreditsField;

    @FXML private Button     btnCollapse;
    @FXML private TextField  txtDegree;
    @FXML private TextField  txtMaxDegree;
    @FXML private Button     btnExpand;
    @FXML private ComboBox<String> cmbHighlight;
    private Consumer<Integer> onDegreeChanged;
    private Consumer<String> onHighlightChanged;
    private final IntegerProperty maxDegree     = new SimpleIntegerProperty(0);
    private final IntegerProperty currentDegree = new SimpleIntegerProperty(0);
    private final ObjectProperty<Runnable> onExpand   = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onCollapse = new SimpleObjectProperty<>();
    private final ObjectProperty<Runnable> onApplyDegree = new SimpleObjectProperty<>();
    private static final Pattern X_IN_ARGS = Pattern.compile("\\bx(\\d+)\\b");
    private static final Pattern Z_IN_ARGS = Pattern.compile("\\bz(\\d+)\\b");

    @FXML
    private void initialize() {
        if (availableCreditsField != null) {
            availableCreditsField.setFocusTraversable(false);
        }

        // current degree (עם ה-formatter הקיים שלך)
        txtDegree.setTextFormatter(integerOnlyFormatter());
        txtDegree.setText("0");

        txtMaxDegree.setEditable(false);
        txtMaxDegree.setFocusTraversable(false);
        txtMaxDegree.textProperty().bind(
                Bindings.createStringBinding(() -> Integer.toString(maxDegree.get()), maxDegree)
        );

        cmbHighlight.getItems().setAll("None", "Instruction", "Block", "Function");
        cmbHighlight.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (onHighlightChanged != null && nv != null) onHighlightChanged.accept(nv);
        });

        refreshButtons();
        txtDegree.textProperty().addListener((o, ov, nv) -> {
            int val;
            try { val = Integer.parseInt(nv.trim()); } catch (Exception e) { val = 0; }
            int clamped = Math.max(0, Math.min(val, maxDegree.get()));
            if (currentDegree.get() != clamped) currentDegree.set(clamped);
            refreshButtons();
        });
        maxDegree.addListener((o, ov, nv) -> refreshButtons());
    }

    @FXML
    private void onExpandClicked() {
        Runnable r = onExpand.get();
        if (r != null) r.run();
    }

    @FXML
    private void onCollapseClicked() {
        Runnable r = onCollapse.get();
        if (r != null) r.run();
    }

    @FXML
    private void onDegreeEnter() {
        Runnable r = onApplyDegree.get();
        if (r != null) r.run();
    }

    public void setMaxDegree(int d) {
        int m = Math.max(0, d);
        maxDegree.set(m);
        refreshButtons();
    }

    public void setOnExpand(Runnable r) {
        this.onExpand.set(r);
    }

    public void setOnCollapse(Runnable r) {
        this.onCollapse.set(r);
    }

    public void setCurrentDegree(int degree) {
        int clamped = Math.max(0, Math.min(degree, maxDegree.get()));
        this.currentDegree.set(clamped);
        if (!String.valueOf(clamped).equals(txtDegree.getText())) {
            txtDegree.setText(Integer.toString(clamped));
        }
        refreshButtons();
    }

    public void setOnDegreeChanged(Consumer<Integer> callback) {
        this.onDegreeChanged = callback;
    }

    public void setOnApplyDegree(Runnable r) { this.onApplyDegree.set(r); }

    public void setUserName(String name) { userNameLabel.setText(name); }
    public void setAvailableCredits(int credits) {
        availableCreditsField.setText(Integer.toString(credits)); }

    public void setRunTarget(String txt) {
        if (runTargetLabel != null) runTargetLabel.setText(txt != null ? txt : "");
    }
    public void setHighlightOptions(List<String> options) {
        cmbHighlight.getItems().setAll(options);
    }

    public int getCurrentDegree() {
        return currentDegree.get();
    }

    public String getSelectedHighlight() {
        String v = (cmbHighlight != null) ? cmbHighlight.getValue() : null;
        return (v == null || v.isBlank() || "Highlight selection".equals(v)) ? "NONE" : v;
    }

    public void selectHighlight(String value) { cmbHighlight.getSelectionModel().select(value); }
    public void setOnHighlightChanged(Consumer<String> c){ this.onHighlightChanged = c; }

    public void populateHighlight(List<InstructionDTO> rows) { populateHighlight(rows, false); }

    public void populateHighlight(List<InstructionDTO> rows, boolean resetToNone) {
        if (cmbHighlight == null) return;
        String prev = resetToNone ? null : cmbHighlight.getValue();
        boolean hasY = false, jumpToExit = false;
        SortedSet<Integer> xs = new TreeSet<>();
        SortedSet<Integer> zs = new TreeSet<>();
        SortedSet<Integer> ls = new TreeSet<>();

        if (rows != null) {
            for (InstructionDTO ins : rows) {
                if (ins == null) continue;

                LabelDTO lbl = ins.getLabel();
                if (lbl != null && !lbl.isExit()) {
                    String name = lbl.getName();
                    if (name != null && name.startsWith("L")) {
                        try { ls.add(Integer.parseInt(name.substring(1))); } catch (Exception ignore) {}
                    }
                }
                InstructionBodyDTO body = ins.getBody();
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

                if (body.getOp() == InstrOpDTO.QUOTE || body.getOp() == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    String argsText = body.getFunctionArgs();
                    parseVarsFromArgs(argsText, xs, zs);
                }
            }
        }

        List<String> items = new ArrayList<>();
        items.add("Highlight selection");
        if (hasY) items.add("y");
        for (Integer i : xs) items.add("x" + i);
        for (Integer i : zs) items.add("z" + i);
        for (Integer i : ls) items.add("L" + i);
        if (jumpToExit) items.add("EXIT");

        cmbHighlight.getItems().setAll(items);
        if (prev != null && items.contains(prev)) {
            cmbHighlight.setValue(prev);
        } else {
            cmbHighlight.setValue("Highlight selection");
        }
    }

    // ===== helpers =====
    private void clampDegree() {
        if (currentDegree.get() > maxDegree.get()) currentDegree.set(maxDegree.get());
        if (currentDegree.get() < 0) currentDegree.set(0);
    }

    private void refreshButtons() {
        int val;
        try { val = Integer.parseInt(txtDegree.getText().trim()); } catch (Exception e) { val = 0; }
        btnCollapse.setDisable(val <= 0);
        btnExpand.setDisable(val >= maxDegree.get());
    }

    private static TextFormatter<Integer> integerOnlyFormatter() {
        return new TextFormatter<>(new StringConverter<>() {
            @Override public String toString(Integer object) { return object == null ? "0" : object.toString(); }
            @Override public Integer fromString(String string) {
                if (string == null || string.isBlank()) return 0;
                try { return Integer.parseInt(string.trim()); } catch (NumberFormatException e) { return 0; }
            }
        });
    }

    private static void parseVarsFromArgs(String text, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (text == null || text.isBlank()) return;
        Matcher mx = X_IN_ARGS.matcher(text);
        while (mx.find()) { try { xs.add(Integer.parseInt(mx.group(1))); } catch (Exception ignore) {} }
        Matcher mz = Z_IN_ARGS.matcher(text);
        while (mz.find()) { try { zs.add(Integer.parseInt(mz.group(1))); } catch (Exception ignore) {} }
    }

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
}
