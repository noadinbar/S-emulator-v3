import api.XMLLoader;
import structure.program.ProgramData;
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

            // 3) בניית ProgramData (קלטים + תוויות) כמו validate
            ProgramData data = ProgramData.build(program);

            // 4) הדפסת כותרות (שם תוכנית, קלטים, תוויות)
            System.out.println("Program: " + program.getName());

            String inputsLine = data.getInputs().isEmpty()
                    ? "-"
                    : String.join(", ", data.getInputs());
            System.out.println("Inputs used: " + inputsLine);

            String labelsLine = data.getLabels().isEmpty()
                    ? "-"
                    : String.join(", ", data.getLabels());
            System.out.println("Labels used: " + labelsLine);
            System.out.println();

            // 5) הדפסת הוראות התוכנית בפורמט התקני (שורה לכל הוראה)
            String instructionsText = ProgramDisplay.renderInstructions(program);
            System.out.print(instructionsText);

        } catch (Exception e) {
            System.err.println("שגיאה בהרצה: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
