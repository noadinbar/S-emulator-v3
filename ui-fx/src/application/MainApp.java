package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

// ממשקי המנוע וה-DTO לבדיקה ידנית
import api.LoadAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
import exportToDTO.LoadAPIImpl;
import display.Command2DTO;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // טוען את ה-FXML הראשי
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("program_scene.fxml"));
        Parent root = fxml.load();
        ProgramSceneController controller = fxml.getController();

        Scene scene = new Scene(root); // גודל פתיחה מה-pref ב-FXML

        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.sizeToScene(); // מאמץ את ה-pref מה-FXML

        // יציאה ב-ESC וב-X
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> Platform.exit();
            }
        });
        stage.setOnCloseRequest(e -> Platform.exit());

        stage.setTitle("S-Emulator");
        stage.centerOnScreen();
        stage.show();

        // לא טוענים XML קשיח — מחכים לטעינה דרך ההדר:
        // tinySmokeTestLoad(controller);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
