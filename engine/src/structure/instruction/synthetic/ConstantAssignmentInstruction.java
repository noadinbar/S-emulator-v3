package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class ConstantAssignmentInstruction extends AbstractInstruction {

    private final int constant;

    public ConstantAssignmentInstruction(Variable variable, int source) {
        super(InstructionKind.SYNTHETIC, InstructionType.CONSTANT_ASSIGNMENT, variable);
        this.constant = source;
    }

    public ConstantAssignmentInstruction(Variable variable, int source, Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.CONSTANT_ASSIGNMENT, variable, myLabel);
        this.constant = source;
    }
    public int getConstant() {
        return constant;
    }
    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), constant);

        return FixedLabel.EMPTY;
    }

    @Override
    public String formatDisplay() {
        String dst = (getVariable() == null) ? "" : getVariable().getRepresentation();
        return String.format("%s <- %s", dst, getConstant());
    }
}
