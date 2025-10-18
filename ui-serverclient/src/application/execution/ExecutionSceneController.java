package application.execution;

import java.util.List;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import application.execution.header.HeaderController;
import application.execution.inputs.InputsController;
import application.execution.outputs.OutputsController;
import application.execution.run.options.RunOptionsController;
import application.execution.summary.SummaryController;
import application.execution.table.instruction.InstructionsController;
import javafx.stage.Stage;

public class ExecutionSceneController {

    // root + centering כמו ב-ui-fx
    @FXML private ScrollPane rootScroll;
    @FXML private VBox contentRoot;

    // fx:include controllers (שם fx:id + "Controller")
    @FXML private HeaderController          headerController;
    @FXML private InstructionsController    programTableController;
    @FXML private SummaryController         summaryController;
    @FXML private InstructionsController    chainTableController;
    @FXML private RunOptionsController      runOptionsController;
    @FXML private OutputsController         outputsController;
    @FXML private InputsController          inputsController;

    // UI: ארכיטקטורה + Back
    @FXML private ComboBox<String> cmbArchitecture;
    @FXML private Button btnBackToOpening;

    // callbacks (שלד בלבד)
    private Runnable onBackToOpening;
    private Consumer<String> onArchitectureSelected;

    @FXML
    private void initialize() {
        // מיקום/מרכוז התוכן בתוך ה-ScrollPane — אחד לאחד מה-ui-fx
        rootScroll.setFitToWidth(false);
        rootScroll.setFitToHeight(false);
        contentRoot.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        contentRoot.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        DoubleBinding viewportW = Bindings.selectDouble(rootScroll.viewportBoundsProperty(), "width");
        DoubleBinding viewportH = Bindings.selectDouble(rootScroll.viewportBoundsProperty(), "height");
        contentRoot.translateXProperty().bind(
                Bindings.max(0, viewportW.subtract(contentRoot.widthProperty()).divide(2))
        );
        contentRoot.translateYProperty().bind(
                Bindings.max(0, viewportH.subtract(contentRoot.heightProperty()).divide(2))
        );

        // שינוי ארכיטקטורה — שלד (רק callback)
        if (cmbArchitecture != null) {
            cmbArchitecture.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (onArchitectureSelected != null) onArchitectureSelected.accept(nv);
            });
        }
    }
    @FXML
    private void onBackToOpening() {
        try {
            openOpeningAndReplace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openOpeningAndReplace() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/opening/opening_scene.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) rootScroll.getScene().getWindow();
        stage.setTitle("S-emulator");
        stage.setScene(new Scene(root));
        stage.show();
    }

    // ===== API לשימוש האפליקציה בהמשך =====
    public void setArchitectureOptions(List<String> options) {
        cmbArchitecture.getItems().setAll(options);
    }
    public void selectArchitecture(String value) {
        cmbArchitecture.getSelectionModel().select(value);
    }
    public String getSelectedArchitecture() {
        return cmbArchitecture.getSelectionModel().getSelectedItem();
    }

    public void setOnBackToOpening(Runnable cb) { this.onBackToOpening = cb; }
    public void setOnArchitectureSelected(Consumer<String> cb) { this.onArchitectureSelected = cb; }
    public void setDebugMode(boolean debug) {}
}
