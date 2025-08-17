package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class ZeroVariableInstruction extends AbstractInstruction {

    public ZeroVariableInstruction(Variable variable) {
        super(InstructionKind.SYNTHETIC, InstructionType.ZERO_VARIABLE, variable);
    }

    public ZeroVariableInstruction(Variable variable, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.ZERO_VARIABLE, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), 0);

        return FixedLabel.EMPTY;
    }
}
