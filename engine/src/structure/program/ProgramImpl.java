package structure.program;

import structure.instruction.Instruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import utils.ParseResult;
import utils.RunHistory;

import java.util.*;

public class ProgramImpl implements Program {

    private final String name;
    private final List<Instruction> instructions;
    private final List<RunHistory> runHistory = new ArrayList<>();
    private int currentRunDegree = 0; // אם לא משתמשים בדרגות, פשוט יישאר 0


    public ProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public ParseResult validate() {
        Set<String> definedLabels = new HashSet<>();
        for (Instruction instr : instructions) {
            Label label = instr.getMyLabel();
            if (label != null && label != FixedLabel.EMPTY) {
                definedLabels.add(label.getLabelRepresentation());
            }
        }

        for (Instruction instr : instructions) {
            Label targetLabel = null;

            if (instr instanceof JumpNotZeroInstruction) {
                targetLabel = ((JumpNotZeroInstruction) instr).getTargetLabel();
                System.out.println("target label: " + targetLabel);
            }

            if (targetLabel != null &&
                    targetLabel != FixedLabel.EMPTY &&
                    targetLabel != FixedLabel.EXIT &&
                    !definedLabels.contains(targetLabel.getLabelRepresentation())) {

                return ParseResult.error(
                        "Label '" + targetLabel.getLabelRepresentation() + "' is referenced but not defined."
                );
            }
        }

        return ParseResult.success("Program validation passed successfully.");
    }

    public void setCurrentRunDegree(int degree) { this.currentRunDegree = Math.max(0, degree); }

    public int getCurrentRunDegree() { return currentRunDegree; }

    public void addRunHistory(List<Long> inputs, long yValue, int cycles) {
        int runNumber = runHistory.size() + 1;
        runHistory.add(new RunHistory(runNumber, currentRunDegree, inputs, yValue, cycles));
    }

    public List<RunHistory> getRunHistory() {
        return Collections.unmodifiableList(runHistory);
    }

    public void clearRunHistory() { runHistory.clear(); }

    //need to implement
    @Override
    public int calculateMaxDegree() {
        // traverse all commands and find maximum degree
        return 0;
    }

    //need to implement
    @Override
    public int calculateCycles() {
        // traverse all commands and calculate cycles
        return 0;
    }


}
