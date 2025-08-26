package structure.instruction.basic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class DecreaseInstruction extends AbstractInstruction {

    public DecreaseInstruction(Variable variable) {
        super(InstructionKind.BASIC, InstructionType.DECREASE, variable, 0);
    }

    public DecreaseInstruction(Variable variable, Label label) {
        super(InstructionKind.BASIC, InstructionType.DECREASE, variable, label, 0);
    }

    @Override
    public Label execute(ExecutionContext context) {

        long variableValue = context.getVariableValue(getVariable());
        variableValue = Math.max(0, variableValue - 1);
        context.updateVariable(getVariable(), variableValue);

        return FixedLabel.EMPTY;
    }

}