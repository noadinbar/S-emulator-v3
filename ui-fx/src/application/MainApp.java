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

// ממשקי המנוע וה-DTO אצלך
import api.LoadAPI;
import api.DisplayAPI;
import exportToDTO.LoadAPIImpl;
import display.Command2DTO;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // טוען את ה-FXML הראשי (נמצא תחת application/program_scene.fxml)
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("program_scene.fxml"));
        Parent root = fxml.load();

        application.ProgramSceneController controller = fxml.getController();
        Scene scene = new Scene(root); // גודל פתיחה יילקח מה-pref ב-FXML
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.sizeToScene(); // מאמץ את ה-pref מה-FXML

        // יציאה ב-ESC וב-X
        scene.setOnKeyPressed(e -> { if ("ESCAPE".equals(e.getCode().toString())) Platform.exit(); });
        stage.setOnCloseRequest(e -> Platform.exit());

        stage.setTitle("S-Emulator");
        stage.centerOnScreen();
        stage.show();

        // בדיקת טעינה מינימלית + הצגת פקודה 2 בטבלה
        tinySmokeTestLoad(controller);
    }

    // טוען XML נתון, מביא Command2DTO, מדפיס לקונסול, ומציג בטבלה דרך הקונטרולר הראשי
    private void tinySmokeTestLoad(application.ProgramSceneController controller) {
        Path xml = Paths.get("C:\\Users\\luzon\\Desktop\\projects\\java\\Project-java\\synthetic.xml");
        System.out.println("[SMOKE] Loading XML: " + xml);
        try {
            LoadAPI loader = new LoadAPIImpl();
            DisplayAPI display = loader.loadFromXml(xml);
            Command2DTO dto = display.getCommand2();

            // >>> תוספת: מזרים את ה-display לקונטרולר הראשי (נדרש עבור Start)
            controller.setDisplay(display);

            System.out.println("[OK] Loaded. DTO != null: " + (dto != null));
            if (dto != null) {
                System.out.println("[OK] DTO type: " + dto.getClass().getSimpleName());
                // מציג את פקודה 2 בטבלת ההוראות (Instructions)
                controller.showCommand2(dto);
            }
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
