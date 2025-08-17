package structure.program;

import structure.instruction.Instruction;
import structure.instruction.JumpNotZeroInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import utils.ParseResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgramImpl implements Program {

    private final String name;
    private final List<Instruction> instructions;


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
