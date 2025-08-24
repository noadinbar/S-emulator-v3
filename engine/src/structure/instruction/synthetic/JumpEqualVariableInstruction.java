package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class JumpEqualVariableInstruction extends AbstractInstruction {
    private final Variable toCompare;
    private final Label targetLabel;

    public JumpEqualVariableInstruction(Variable variable, Label jevLabel, Variable toCompare) {
        this(variable, jevLabel, toCompare, FixedLabel.EMPTY);

    }

    public JumpEqualVariableInstruction(Variable variable, Label jevLabel, Variable toCompare, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_VARIABLE, variable, label);
        this.targetLabel = jevLabel;
        this.toCompare = toCompare;
    }
    public Variable getToCompare() {
        return toCompare;
    }
    public Label getTargetLabel() {
        return targetLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());
        long toCompareValue = context.getVariableValue(toCompare);

        if (variableValue==toCompareValue) {
            return targetLabel;
        }
        return FixedLabel.EMPTY;
    }
}
