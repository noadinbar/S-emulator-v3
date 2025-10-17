package application.opening.functions;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

public class FunctionsController {

    @FXML private BorderPane functionsRoot;
    @FXML private Label titleLabel;
    @FXML private ListView<String> functionsListView;
    @FXML private Button executeBtn;

    private final ObservableList<String> functions = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        functionsListView.setItems(functions);
        if (executeBtn != null) {
            executeBtn.disableProperty().bind(
                    functionsListView.getSelectionModel().selectedItemProperty().isNull()
            );
        }
    }

    // API
    public void setFunctions(List<String> newFunctions) { functions.setAll(newFunctions); }
    public void addFunction(String fn) { if (!functions.contains(fn)) functions.add(fn); }
    public void removeFunction(String fn) { functions.remove(fn); }
    public String getSelectedFunction() { return functionsListView.getSelectionModel().getSelectedItem(); }
    public void selectFunction(String fn) {
        functionsListView.getSelectionModel().select(fn);
        functionsListView.scrollTo(fn);
    }

    @FXML
    private void onExecuteAction() { functionsListView.getSelectionModel().clearSelection(); }
}
