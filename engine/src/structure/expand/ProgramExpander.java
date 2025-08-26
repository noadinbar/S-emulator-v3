package structure.expand;

import java.util.ArrayList;
import java.util.List;

import structure.instruction.Instruction;
import structure.program.Program;
import structure.program.ProgramImpl;

public final class ProgramExpander {

    private ProgramExpander() {}

    /**
     * מרחיב את התוכנית במספר דרגות.
     * בכל דרגה: מריצים expand(...) על כל ההוראות, ואוספים את הכל ל-ProgramImpl חדש.
     */
    public static Program expandTo(Program baseProgram, int degree) {
        if (degree == 0) return baseProgram;

        // עובדים עם ProgramImpl כי שם יש את עזרי ה-max
        ProgramImpl current = (ProgramImpl) baseProgram;

        // מאתחלים ExpansionManager פעם אחת לכל הסשן (L#, z# רציפים)
        int nextLabel = current.findMaxLabelIndex() + 1;
        int nextWork  = current.findMaxWorkIndex() + 1;
        ExpansionManager mgr = new ExpansionManagerImpl(nextLabel, nextWork);

        for (int d = 0; d < degree; d++) {
            List<Instruction> nextInstructions = new ArrayList<>();
            for (Instruction ins : current.getInstructions()) {
                nextInstructions.addAll(ins.expand(mgr));
            }

            ProgramImpl nextProgram = new ProgramImpl(current.getName());
            for (Instruction e : nextInstructions) {
                nextProgram.addInstruction(e);
            }
            current = nextProgram;
        }

        return current;
    }
}
