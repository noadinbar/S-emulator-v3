// File: engine/src/structure/expand/ExpandResult.java
package structure.expand;

import structure.instruction.Instruction;
import structure.program.Program;

import java.util.ArrayList;
import java.util.List;

public final class ExpandResult {
    private final Program expandedProgram;
    private final List<List<Instruction>> levels;

    public ExpandResult(Program expandedProgram, List<List<Instruction>> levels) {
        this.expandedProgram = expandedProgram;
        this.levels = deepUnmodifiable(levels);
    }

    public Program getExpandedProgram() {
        return expandedProgram;
    }

    public List<List<Instruction>> getLevels() {
        return levels;
    }

    private static List<List<Instruction>> deepUnmodifiable(List<List<Instruction>> src) {
        if (src == null || src.isEmpty()) return List.of();
        List<List<Instruction>> out = new ArrayList<>(src.size());
        for (List<Instruction> level : src) {
            out.add(level == null ? List.of() : List.copyOf(level));
        }
        return List.copyOf(out);
    }
}
