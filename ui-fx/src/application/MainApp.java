package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;       // 👈 חשוב
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("program_scene.fxml"));
        Parent root = fxml.load();                 // 👈 לא var—מוגדר כ-Parent

        Scene scene = new Scene(root);             // ייקח את ה-prefSize מה-FXML
        stage.initStyle(StageStyle.DECORATED);
        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.sizeToScene();                       // מאמץ את הגדלים מה-FXML

        // יציאה ב-ESC וב-X
        scene.setOnKeyPressed(e -> { if (e.getCode().toString().equals("ESCAPE")) Platform.exit(); });
        stage.setOnCloseRequest(e -> Platform.exit());

        stage.setTitle("S-Emulator");
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
