package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class ZeroVariableInstruction extends AbstractInstruction {

    public ZeroVariableInstruction(Variable variable) {
        super(InstructionKind.SYNTHETIC, InstructionType.ZERO_VARIABLE, variable,1);
    }

    public ZeroVariableInstruction(Variable variable, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.ZERO_VARIABLE, variable, label,1);
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), 0);

        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Label myLabel   = getMyLabel();
        Label currLabel = (myLabel == FixedLabel.EMPTY) ? prog.newLabel() : myLabel;

        // 1) Lloop: DEC v
        instructions.add(new DecreaseInstruction(getVariable(), currLabel));

        // 2) IF v != 0 GOTO Lloop
        instructions.add(new JumpNotZeroInstruction(getVariable(), currLabel));

        return instructions;
    }

}
