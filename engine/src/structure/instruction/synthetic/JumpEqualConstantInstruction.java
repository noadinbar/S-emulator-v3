package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class JumpEqualConstantInstruction extends AbstractInstruction {
    private final int constant;
    private final Label targetLabel;

    public JumpEqualConstantInstruction(Variable variable, Label jecLabel, int source) {
        this(variable, jecLabel, source, FixedLabel.EMPTY);

    }

    public JumpEqualConstantInstruction(Variable variable, Label jecLabel, int source, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_CONSTANT, variable, label);
        this.targetLabel = jecLabel;
        this.constant = source;
    }
    public int getConstant() {
        return constant;
    }
    public Label getTargetLabel() {
        return targetLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue == constant) {
            return targetLabel;
        }
        return FixedLabel.EMPTY;
    }
}
