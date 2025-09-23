package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.IncreaseInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

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
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Variable v     = getVariable();
        int k          = getConstant();
        Label myLabel  = getMyLabel();

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new ZeroVariableInstruction(v));
        else
            instructions.add(new ZeroVariableInstruction(v, myLabel));

        for (int i = 0; i < k; i++) {
            instructions.add(new IncreaseInstruction(v));
        }

        return instructions;
    }

}
