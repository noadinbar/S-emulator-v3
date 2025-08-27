package structure.expand;

import structure.instruction.Instruction;
import structure.program.Program;
import java.util.List;
import java.util.Map;

/** תוצאת expand: התוכנית המורחבת + רמות + מיפויי יוצר/דרגת יוצר. */
public final class ExpandResult {
    private final Program expandedProgram;
    private final List<List<Instruction>> levels;                // 0..degree
    private final Map<Instruction, Instruction> parentOf;        // child -> creator
    private final Map<Instruction, Integer> creatorLevelByChild; // child -> level(creator)

    public ExpandResult(Program expandedProgram,
                        List<List<Instruction>> levels,
                        Map<Instruction, Instruction> parentOf,
                        Map<Instruction, Integer> creatorLevelByChild) {
        this.expandedProgram = expandedProgram;
        this.levels = levels;
        this.parentOf = parentOf;
        this.creatorLevelByChild = creatorLevelByChild;
    }

    public Program getExpandedProgram() { return expandedProgram; }
    public List<List<Instruction>> getLevels() { return levels; }
    public Map<Instruction, Instruction> getParentOf() { return parentOf; }
    public Map<Instruction, Integer> getCreatorLevelByChild() { return creatorLevelByChild; }
}
