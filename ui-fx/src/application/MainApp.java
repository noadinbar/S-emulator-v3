package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // program_scene.fxml צריך להיות תחת src/.../application/
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("program_scene.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);

        stage.setTitle("S-Emulator");
        stage.setScene(scene);
        stage.show();

        // בדיקת עשן קצרה לקונסולה (לא חובה)
        ProgramSceneController c = loader.getController();
        System.out.println("ProgramScene loaded? " + (c != null));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
