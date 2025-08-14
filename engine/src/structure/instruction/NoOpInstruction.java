package structure.instruction;


import structure.execution.ExecutionContext;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction(Variable variable) {
        super(InstructionType.NO_OP, variable);
    }

    public NoOpInstruction(Variable variable, Label label) {
        super(InstructionType.NO_OP, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;

    }
}
