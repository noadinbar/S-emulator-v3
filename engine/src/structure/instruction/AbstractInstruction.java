package structure.instruction;


import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public abstract class AbstractInstruction implements Instruction {


    private final InstructionType instType;
    private final Label myLabel;
    private final Variable variable;

    public AbstractInstruction(InstructionType type, Variable variable) {
        this(type, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionType type, Variable variable, Label label) {
        this.instType = type;
        this.myLabel = label;
        this.variable = variable;
    }

    @Override
    public String getName() {
        return instType.getName();
    }

    @Override
    public int cycles() {
        return instType.getCycles();
    }

    @Override
    public Label getMyLabel() {
        return myLabel;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }
}
