package structure.instruction.basic;


import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class IncreaseInstruction extends AbstractInstruction {

    public IncreaseInstruction(Variable variable) {
        super(InstructionKind.BASIC, InstructionType.INCREASE, variable, 0);
    }

    public IncreaseInstruction(Variable variable, Label label) {
        super(InstructionKind.BASIC, InstructionType.INCREASE, variable, label, 0);
    }

    @Override
    public Label execute(ExecutionContext context) {

        long variableValue = context.getVariableValue(getVariable());
        variableValue++;
        context.updateVariable(getVariable(), variableValue);

        return FixedLabel.EMPTY;
    }

}