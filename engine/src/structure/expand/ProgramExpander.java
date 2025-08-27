package structure.expand;

import structure.instruction.Instruction;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * מרחיב לדרגה נתונה.
 * בנוסף שומר:
 *  - levels: הוראות בכל דרגה (כולל 0)
 *  - parentOf: child -> creator לפי הכלל: BASIC לעולם לא הורה; ילד של BASIC יורש את היוצר של ההורה (אם קיים)
 *  - creatorLevelByChild: מאיזו דרגה להביא את ה־DTO של היוצר
 */
public final class ProgramExpander {

    private ProgramExpander() {}

    public static ExpandResult expandTo(Program baseProgram, int degree) {
        if (degree == 0) {
            List<List<Instruction>> levels0 = new ArrayList<>();
            levels0.add(new ArrayList<>(baseProgram.getInstructions()));
            return new ExpandResult(baseProgram, levels0,
                    new IdentityHashMap<>(), new IdentityHashMap<>());
        }

        // עובדים עם ProgramImpl כי שם יש את עזרי ה-max
        ProgramImpl current = (ProgramImpl) baseProgram;

        // מאתחלים ExpansionManager פעם אחת לכל הסשן (L#, z# רציפים)
        int nextLabel = current.findMaxLabelIndex() + 1;
        int nextWork  = current.findMaxWorkIndex() + 1;
        ExpansionManager mgr = new ExpansionManagerImpl(nextLabel, nextWork);

        // רמות + מיפויי יוצר
        List<List<Instruction>> levels = new ArrayList<>();
        levels.add(new ArrayList<>(current.getInstructions())); // level 0

        Map<Instruction, Instruction> parentOf = new IdentityHashMap<>();        // child -> creator
        Map<Instruction, Integer>     creatorLevelByChild = new IdentityHashMap<>(); // child -> level(creator)
        Map<Instruction, Integer>     levelOf = new IdentityHashMap<>();         // עזר: הוראה -> דרגה
        for (Instruction ins : current.getInstructions()) levelOf.put(ins, 0);

        for (int d = 0; d < degree; d++) {
            List<Instruction> nextInstructions = new ArrayList<>();

            for (Instruction ins : current.getInstructions()) {
                List<Instruction> children = ins.expand(mgr);

                char parentKind = ins.kind(); // 'B' / 'S'
                for (Instruction child : children) {
                    // דרגת הילד
                    levelOf.put(child, d + 1);

                    Instruction creator = null;
                    if (parentKind == 'B' || parentKind == 'b') {
                        // BASIC לעולם לא הורה – הילד יורש את היוצר של ההורה (אם קיים)
                        creator = parentOf.get(ins); // עשוי להיות null (B מקורי)
                    } else {
                        // ההורה אינו BASIC (כלומר S) ⇒ הוא היוצר, כל עוד לא אותו אובייקט
                        if (child != ins) creator = ins;
                    }

                    if (creator != null) {
                        parentOf.put(child, creator);
                        Integer creatorLevel = levelOf.get(creator);
                        if (creatorLevel == null) creatorLevel = d; // fallback
                        creatorLevelByChild.put(child, creatorLevel);
                    }
                }

                nextInstructions.addAll(children);
            }

            ProgramImpl nextProgram = new ProgramImpl(current.getName());
            for (Instruction e : nextInstructions) nextProgram.addInstruction(e);
            current = nextProgram;

            levels.add(new ArrayList<>(current.getInstructions())); // level d+1
        }

        return new ExpandResult(current, levels, parentOf, creatorLevelByChild);
    }
}
