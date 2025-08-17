package structure.instruction;

import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionKind kind;
    private final InstructionType instType;
    private final Label myLabel;
    private final Variable variable;

    public AbstractInstruction(InstructionKind instKind ,InstructionType type, Variable variable) {
        this(instKind, type, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionKind instKind, InstructionType type, Variable variable, Label label) {
        this.kind=instKind;
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
    public char kind(){return kind.getKind();}

    @Override
    public Label getMyLabel() {
        return myLabel;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

}
