package structure.instruction.basic;


import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction(Variable variable) {
        super(InstructionKind.BASIC, InstructionType.NEUTRAL, variable);
    }

    public NoOpInstruction(Variable variable, Label label) {
        super(InstructionKind.BASIC, InstructionType.NEUTRAL, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;

    }
}
