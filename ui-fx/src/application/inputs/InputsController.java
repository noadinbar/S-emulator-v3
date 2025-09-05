package application.inputs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;

import java.util.ArrayList;
import java.util.List;

public class InputsController {
    private final List<TextField> inputFields = new ArrayList<>();
    private final VBox inputsContainer;

    public InputsController() {
        inputsContainer = new VBox(8); // 8 pixels spacing
        inputsContainer.setPadding(new Insets(10));
    }

    public Parent getRoot() {
        return inputsContainer;
    }

    public void setInputsCount(int count) {
        inputsContainer.getChildren().clear();
        inputFields.clear();

        for (int i = 0; i < count; i++) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER);

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
