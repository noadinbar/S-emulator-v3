import api.XMLLoader;
import structure.program.ProgramData;
import structure.program.SProgram;
import structure.program.ProgramImpl;
import structure.execution.ProgramExecutor;
import structure.execution.ProgramExecutorImpl;
import utils.RunHistory;
import utils.XMLToStructure;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        // נתיב קשיח לקובץ הבדיקה (אפשר להחליף בארגומנט ראשון אם מוסרים)
        String xmlPath = "C:\\Users\\user\\Desktop\\Noa Dinbar\\Java\\test files\\successor.xml";
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


            // 3) ProgramData (קלטים + תוויות)
            ProgramData data = ProgramData.build(program);

            // 4) מידע כללי + הוראות (פקודה 2)
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

            String instructionsText = ProgramDisplay.renderInstructions(program);
            System.out.print(instructionsText);

            // ===== פקודה 4: הרצה 3 פעמים עם קלטים שונים =====
            System.out.println("\n--- Execute x3 (Command 4) ---");

            int nInputs = maxXIndex(data.getInputs()); // כמה x_i יש בתוכנית (מקס' אינדקס)
            ProgramExecutor executor = new ProgramExecutorImpl(program);

            // נגדיר שלוש ריצות: x1=0, x1=5, x1=12 (ושאר ה-x אם יש → 0)
            Long[] r1 = padInputs(new Long[]{0L}, nInputs);
            Long[] r2 = padInputs(new Long[]{5L}, nInputs);
            Long[] r3 = padInputs(new Long[]{12L}, nInputs);

            long y1 = executor.run(r1);
            System.out.println("Run #1 -> inputs=" + Arrays.asList(r1) + " | y0=" + y1);

            long y2 = executor.run(r2);
            System.out.println("Run #2 -> inputs=" + Arrays.asList(r2) + " | y0=" + y2);

            long y3 = executor.run(r3);
            System.out.println("Run #3 -> inputs=" + Arrays.asList(r3) + " | y0=" + y3);

            // הצגת ההיסטוריה שנשמרה (פקודה 5, אם חיברת addRunHistory)
            List<RunHistory> hist = program.getRunHistory();
            if (!hist.isEmpty()) {
                System.out.println("\n--- Run History ---");
                for (RunHistory rec : hist) {
                    System.out.printf("#%d | degree=%d | inputs=%s | y=%d | cycles=%d%n",
                            rec.getRunNumber(), rec.getDegree(), rec.getInputs(), rec.getYValue(), rec.getCycles());
                }
            }

        } catch (Exception e) {
            System.err.println("שגיאה בהרצה: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // מחלץ את אינדקס ה-x הגבוה ביותר מתוך ["x1","x2",...]
    private static int maxXIndex(List<String> xs) {
        int max = 0;
        Pattern p = Pattern.compile("^x(\\d+)$", Pattern.CASE_INSENSITIVE);
        for (String s : xs) {
            if (s == null) continue;
            Matcher m = p.matcher(s.trim());
            if (m.matches()) {
                try { max = Math.max(max, Integer.parseInt(m.group(1))); }
                catch (Exception ignore) {}
            }
        }
        return max == 0 ? 1 : max; // ברירת-מחדל: לפחות x1
    }

    // מרפד קלטים לאורך n: מה שחסר → 0
    private static Long[] padInputs(Long[] given, int n) {
        if (n <= 0) n = 1;
        Long[] arr = new Long[n];
        Arrays.fill(arr, 0L);
        for (int i = 0; i < Math.min(n, given.length); i++) {
            arr[i] = (given[i] == null ? 0L : given[i]);
        }
        return arr;
    }
}
