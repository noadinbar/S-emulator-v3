package application.inputs;

import execution.ExecutionRequestDTO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InputsController implements Initializable {
    @FXML
    private Pane rootPane;

    private final List<TextField> inputFields = new ArrayList<>();
    private VBox inputsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inputsContainer = new VBox(8); // 8 pixels spacing between rows
        inputsContainer.setPadding(new Insets(10));
        rootPane.getChildren().clear();
        rootPane.getChildren().add(inputsContainer);
    }

    public void setInputsCount(int count) {
        inputsContainer.getChildren().clear();
        inputFields.clear();

        for (int i = 0; i < count; i++) {
            HBox row = new HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER);

            Label label = new Label("Input " + (i + 1));
            TextField input = new TextField();
            input.setPrefWidth(69);

            row.getChildren().addAll(label, input);
            inputsContainer.getChildren().add(row);
            inputFields.add(input);
        }
    }

    public List<Long> getInputValues() {
        List<Long> values = new ArrayList<>();
        for (TextField field : inputFields) {
            try {
                values.add(Long.parseLong(field.getText()));
            } catch (NumberFormatException e) {
                values.add(0L); // Default value if parsing fails
            }
        }
        return values;
    }

    public void setInputValues(List<Long> values) {
        if (values == null) return;

        setInputsCount(values.size());
        for (int i = 0; i < values.size() && i < inputFields.size(); i++) {
            inputFields.get(i).setText(String.valueOf(values.get(i)));
        }
    }
}
