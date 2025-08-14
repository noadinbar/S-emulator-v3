package structure.instruction;


import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionType instructionData;
    private final Label label;
    private final Variable variable;

    public AbstractInstruction(InstructionType instructionData, Variable variable) {
        this(instructionData, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionType instructionData, Variable variable, Label label) {
        this.instructionData = instructionData;
        this.label = label;
        this.variable = variable;
    }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.getCycles();
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }
}
