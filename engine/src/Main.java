import api.XMLLoader;
import structure.program.SProgram;
import structure.program.ProgramImpl;
import utils.XMLToStructure;

import jakarta.xml.bind.JAXBException;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try{
        // נתיב לקובץ ה-XML שלך
            String xmlPath = "C:\\Users\\user\\Desktop\\Noa Dinbar\\Java\\test.xml";

            // שלב 1 - טעינת ה-SProgram מה-XML
            XMLLoader loader = new XMLLoader();
            SProgram sProgram = loader.loadFromXml(Paths.get(xmlPath));

            // שלב 2 - המרה ל-ProgramImpl
            XMLToStructure mapper = new XMLToStructure();
            ProgramImpl program = mapper.toProgram(sProgram);

            // שלב 3 - הדפסה כדי לבדוק
            System.out.println("Program name: " + program.getName());
            System.out.println("Instructions:");
            program.getInstructions().forEach(instr -> {
                System.out.println("  " + instr.getClass().getSimpleName() + " -> " + instr);
            });

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
