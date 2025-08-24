package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class JumpZeroInstruction extends AbstractInstruction {
    private final Label targetLabel;

    public JumpZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, jnzLabel, FixedLabel.EMPTY);
    }

    public JumpZeroInstruction(Variable variable, Label jnzLabel, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_ZERO, variable, label);
        this.targetLabel = jnzLabel;
    }

    public Label getTargetLabel() {
        return targetLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue == 0) {
            return targetLabel;
        }
        return FixedLabel.EMPTY;
    }


}
