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

    // --- בדיקות ידניות (נשארות, לא נקראות אוטומטית) ---

    private void tinySmokeTestLoad(ProgramSceneController controller) {
        Path xml = Paths.get("C:\\Users\\luzon\\Desktop\\projects\\java\\Project-java\\synthetic.xml");
        System.out.println("[SMOKE] Loading XML: " + xml);
        try {
            LoadAPI loader = new LoadAPIImpl();
            DisplayAPI display = loader.loadFromXml(xml);
            Command2DTO dto = display.getCommand2();

            // מזרים את ה-display לקונטרולר הראשי (כמו טעינה מה־Header)
            controller.setDisplay(display);
            if (dto != null) controller.showCommand2(dto);

            // tinySmokeTestRun(display); // לבדיקת ריצה ידנית
        } catch (Exception e) {
            System.out.println("[FAIL] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // מריץ execute() עם degree=0 ואינפוטים ריקים (מתורגמים ל-0)
    private void tinySmokeTestRun(DisplayAPI display) {
        try {
            ExecutionAPI exec = display.execution();
            List<Long> inputs = Collections.emptyList(); // שקול לכל xi=0
            ExecutionRequestDTO req = new ExecutionRequestDTO(0, inputs); // degree=0
            ExecutionDTO res = exec.execute(req);
            System.out.println("[SMOKE/RUN] cycles=" + res.getTotalCycles() + ", y=" + res.getyValue());
        } catch (Exception e) {
            System.out.println("[SMOKE/RUN][FAIL] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // עזר קטן לשמות משתנים כמו ב-UI (y / xk / zk)
    private static String formatVarName(VarRefDTO v) {
        if (v == null) return "";
        if (v.getVariable() == VarOptionsDTO.y) return "y";
        String base = (v.getVariable() == VarOptionsDTO.x) ? "x" : "z";
        return base + v.getIndex();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
