package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class AssignmentInstruction extends AbstractInstruction {

    private final Variable toAssign;

    public AssignmentInstruction(Variable dest, Variable source) {
        super(InstructionKind.SYNTHETIC, InstructionType.ASSIGNMENT, dest);
        this.toAssign = source;
    }

    public AssignmentInstruction(Variable dest, Variable source, Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.ASSIGNMENT, dest, myLabel);
        this.toAssign = source;
    }
    public Variable getToAssign() {
        return toAssign;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(toAssign);
        context.updateVariable(getVariable(), variableValue);

        return FixedLabel.EMPTY;
    }

    @Override
    public String formatDisplay() {
        String dst = (getVariable() == null) ? "" : getVariable().getRepresentation();
        String src = (getToAssign() == null) ? "" : getToAssign().getRepresentation();
        return String.format("%s <- %s", dst, src);
    }
}
