package structure.expand;

import structure.instruction.Instruction;
import structure.instruction.AbstractInstruction;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.util.ArrayList;
import java.util.List;

public final class ProgramExpander {

    private ProgramExpander() {}

    // מחזיר ExpandResult: התוכנית בדרגה האחרונה + כל הדרגות (0..degree)
    public static ExpandResult expandTo(Program baseProgram, int degree) {
        if (degree == 0) {
            List<List<Instruction>> levels0 = new ArrayList<>();
            levels0.add(new ArrayList<>(baseProgram.getInstructions())); // level 0
            return new ExpandResult(baseProgram, levels0);
        }

        // עובדים עם ProgramImpl כי שם יש את עזרי ה-max
        ProgramImpl current = (ProgramImpl) baseProgram;

        // מאתחלים ExpansionManager פעם אחת לכל הסשן (L#, z# רציפים)
        int nextLabel = current.findMaxLabelIndex() + 1;
        int nextWork  = current.findMaxWorkIndex() + 1;
        ExpansionManager mgr = new ExpansionManagerImpl(nextLabel, nextWork);

        // נשמור גם את הדרגות (לצורך DTO של פקודה 3)
        List<List<Instruction>> levels = new ArrayList<>();
        levels.add(new ArrayList<>(current.getInstructions())); // level 0

        for (int d = 0; d < degree; d++) {
            List<Instruction> nextInstructions = new ArrayList<>();

            for (Instruction ins : current.getInstructions()) {
                List<Instruction> children = ins.expand(mgr);

                // BASIC לא מתרחב; "הורה אמיתי" תמיד הוראה לא-Basic (לרוב S)
                final List<Instruction> parentChain = ((AbstractInstruction) ins).getFamilyTree();

                for (Instruction child : children) {
                    if (child == ins) {
                        // לא נוצר ילד חדש (לרוב BASIC “עובר קדימה”) — לא משנים שרשרת
                        continue;
                    }
                    // הורה יוצר: familyTree של ילד = [ins] + familyTree של ההורה
                    AbstractInstruction currentInstruction = (AbstractInstruction) child;
                    List<Instruction> chain = new ArrayList<>(1 + parentChain.size());
                    chain.add(ins);
                    chain.addAll(parentChain);
                    currentInstruction.setFamilyTree(chain);
                }

                nextInstructions.addAll(children);
            }

            ProgramImpl nextProgram = new ProgramImpl(current.getName());
            for (Instruction inputInstruction : nextInstructions) {
                nextProgram.addInstruction(inputInstruction);
            }
            current = nextProgram;

            levels.add(new ArrayList<>(current.getInstructions())); // מוסיפים דרגה
        }

        return new ExpandResult(current, levels);
    }
}
