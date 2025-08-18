package utils;

import structure.instruction.Instruction;
import structure.label.Label;
import structure.program.ProgramImpl;

import java.util.List;

public final class ProgramDisplay {
    private static final int LABEL_WIDTH = 5;

    private ProgramDisplay() {}

    /** מחזיר את רשימת ההוראות בלבד, לפי סדר הופעתן בקובץ ה-XML. */
    public static String renderInstructions(ProgramImpl program) {
        if (program == null) {
            return String.format("no program is loaded%n");
        }
        StringBuilder out = new StringBuilder();
        List<Instruction> ins = program.getInstructions();
        for (int i = 0; i < ins.size(); i++) {
            out.append(String.format("%s%n", renderInstructionLine(i + 1, ins.get(i))));
        }
        return out.toString();
    }

    /** שורה אחת בפורמט: #<number> (B|S) [LABEL] <command> (cycles) */
    public static String renderInstructionLine(int index1, Instruction inst) {
        // LABEL ברוחב 5 תווים (גם אם אין תווית)
        String labelText = "";
        Label lab = inst.getMyLabel();
        if (lab != null && lab.getLabelRepresentation() != null) {
            labelText = lab.getLabelRepresentation();
        }
        String fixedLabel = String.format("%-" + LABEL_WIDTH + "s", labelText);

        // ה־<command> מגיע מהמימוש שלך בכל מחלקת פקודה
        String command = inst.formatDisplay();

        // הרכבה סופית
        return String.format("#%d (%c) [ %s ] %s (%d)",
                index1, inst.kind(), fixedLabel, command, inst.cycles());
    }
}
