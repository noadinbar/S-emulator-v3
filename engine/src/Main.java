import api.XMLLoader;
import structure.program.SProgram;
import structure.program.ProgramImpl;
import utils.XMLToStructure;
import utils.ProgramDisplay;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // נתיב קשיח לקובץ הבדיקה (אפשר להחליף בארגומנט ראשון אם מוסרים)
        String xmlPath = "C:\\Users\\user\\Desktop\\Noa Dinbar\\Java\\test files\\synthetic.xml";
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            xmlPath = args[0];
        }

        try {
            // 1) טעינת S-Program מה-XML
            XMLLoader loader = new XMLLoader();
            SProgram sProgram = loader.loadFromXml(Paths.get(xmlPath));

            // 2) המרה ל-ProgramImpl
            XMLToStructure mapper = new XMLToStructure();
            ProgramImpl program = mapper.toProgram(sProgram);

            // 3) שימוש בפורמט ההצגה של פקודה 2 (הוראות התוכנית בלבד)
            String instructionsText = ProgramDisplay.renderInstructions(program);

            // 4) הדפסה למסך (בדיקה זריזה)
            System.out.print(instructionsText);

        } catch (Exception e) {
            System.err.println("שגיאה בהרצה: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
