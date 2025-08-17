import api.XMLLoader;
import structure.instruction.Instruction;
import structure.label.Label;
import structure.program.SProgram;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import utils.XMLToStructure;

import jakarta.xml.bind.JAXBException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            // נתיב לקובץ ה-XML שלך
            String xmlPath = (args.length > 0) ? args[0] : "C:\\Users\\user\\Desktop\\Noa Dinbar\\Java\\test.xml";

            // שלב 1 - טעינת ה-SProgram מה-XML
            XMLLoader loader = new XMLLoader();
            SProgram sProgram = loader.loadFromXml(Paths.get(xmlPath));

            // שלב 2 - המרה ל-ProgramImpl
            XMLToStructure mapper = new XMLToStructure();
            ProgramImpl program = mapper.toProgram(sProgram);

            // שלב 3 - הדפסה מלאה של התוכנית
            System.out.println("Program: " + program.getName());
            List<Instruction> ins = program.getInstructions();

            int basic = 0, synthetic = 0, totalCycles = 0;

            for (int i = 0; i < ins.size(); i++) {
                Instruction inst = ins.get(i);

                char kind = inst.kind(); // 'B' / 'S'
                if (kind == 'B') basic++; else synthetic++;

                Label lbl = inst.getMyLabel();
                String labelRep = (lbl != null && !lbl.getLabelRepresentation().isEmpty())
                        ? "[" + lbl.getLabelRepresentation() + "] "
                        : "";

                String op = inst.getName();

                Variable v = inst.getVariable();
                String varRep = (v != null) ? v.getRepresentation() : "";

                String argsStr = buildArgsString(inst);

                int cycles = inst.cycles();
                totalCycles += cycles;

                String line = String.format(
                        "# %d (%c) %s%s%s%s (cycles=%d)",
                        i + 1,
                        kind,
                        labelRep,
                        op,
                        varRep.isEmpty() ? "" : " " + varRep,
                        argsStr.isEmpty() ? "" : "  { " + argsStr + " }",
                        cycles
                );
                System.out.println(line);
            }

            System.out.printf(
                    "%nSummary: %d instructions | BASIC=%d, SYNTHETIC=%d | total cycles=%d%n",
                    ins.size(), basic, synthetic, totalCycles
            );

            // ולידציה
            System.out.println(program.validate().getMessage());

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /* ================== Helpers ================== */

    // בונה מחרוזת ארגומנטים ע"י ניסוי קריאה של גטרים נפוצים בפקודות,
    // ובמיוחד דואגת שב-GOTO_LABEL יופיע goto=TARGET גם אם שם הגטר/השדה שונה.
    private static String buildArgsString(Instruction inst) {
        List<String> parts = new ArrayList<>();

        // מקור/השמה (ASSIGNMENT / CONSTANT_ASSIGNMENT וכו')
        Object srcVar = tryGet(inst,
                "getToAssign", "getSource", "getAssignedVariable", "getFrom", "getSourceVariable");
        if (srcVar instanceof Variable sv) {
            parts.add("from=" + safeVar(sv));
        }

        // משתנה להשוואה (JUMP_EQUAL_VARIABLE)
        Object cmpVar = tryGet(inst,
                "getToCompare", "getCompareVariable", "getVariableToCompare");
        if (cmpVar instanceof Variable cv) {
            parts.add("compare=" + safeVar(cv));
        }

        // תוויות יעד (קפיצות) – ניסיון דרך גטרים נפוצים
        Object tgtLbl = tryGet(inst,
                "getTargetLabel", "getGoToLabel", "getDestinationLabel",
                "getJumpLabel", "getJEConstantLabel", "getJEVariableLabel");
        if (tgtLbl instanceof Label tl) {
            String rep = safeLabel(tl);
            if (!rep.isEmpty()) parts.add("goto=" + rep);
        }

        // קבועים (השמה/השוואה)
        Object constVal = tryGet(inst, "getConstant", "getConstantValue");
        if (constVal instanceof Number n) {
            parts.add("value=" + n.longValue());
        }

        // יעד ל-JNZ אם בשם נפרד
        Object jnzLbl = tryGet(inst, "getJNZLabel");
        if (jnzLbl instanceof Label jl) {
            String rep = safeLabel(jl);
            if (!rep.isEmpty()) parts.add("jnz=" + rep);
        }

        // --- טיפול מפורש ב-GOTO_LABEL אם עדיין לא מצאנו goto= ---
        boolean alreadyHasGoto = parts.stream().anyMatch(s -> s.startsWith("goto="));
        boolean looksLikeGoto = "GOTO_LABEL".equalsIgnoreCase(inst.getName())
                || inst.getClass().getSimpleName().toLowerCase().contains("goto");
        if (looksLikeGoto && !alreadyHasGoto) {
            Object fldLbl = tryGetField(inst, "goToLabel", "targetLabel", "destinationLabel");
            if (fldLbl instanceof Label fl) {
                String rep = safeLabel(fl);
                if (!rep.isEmpty()) parts.add("goto=" + rep);
            }
        }

        return String.join(", ", parts);
    }

    // מנסה לקרוא מתודה ללא פרמטרים, אם קיימת
    private static Object tryGet(Object target, String... methodNames) {
        Class<?> cls = target.getClass();
        for (String name : methodNames) {
            try {
                Method m = cls.getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (NoSuchMethodException e) {
                // נמשיך לשם הבא
            } catch (Exception e) {
                // לא נפליל — פשוט לא נדפיס את הפרמטר הזה
            }
        }
        return null;
    }

    // ניסיון להביא ערך ישירות משדה (אם אין גטרים)
    private static Object tryGetField(Object target, String... fieldNames) {
        Class<?> cls = target.getClass();
        for (String name : fieldNames) {
            try {
                var f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                // נמשיך לשם הבא
            } catch (Exception e) {
                // מתעלמים – אין הדפסה של הפרמטר הזה
            }
        }
        return null;
    }

    private static String safeVar(Variable v) {
        try {
            return (v == null) ? "" : v.getRepresentation();
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    private static String safeLabel(Label l) {
        try {
            return (l == null) ? "" : l.getLabelRepresentation();
        } catch (Exception e) {
            return String.valueOf(l);
        }
    }
}
