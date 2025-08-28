package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class JumpZeroInstruction extends AbstractInstruction {
    private final Label targetLabel;

    public JumpZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, jnzLabel, FixedLabel.EMPTY);
    }

    public JumpZeroInstruction(Variable variable, Label jnzLabel, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_ZERO, variable, label, 2);
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
    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Label myLabel = getMyLabel();
        Label skip    = prog.newLabel();
        Label target  = getTargetLabel();

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new JumpNotZeroInstruction(getVariable(), skip));
        else
            instructions.add(new JumpNotZeroInstruction(getVariable(), skip, myLabel));

        instructions.add(new GoToInstruction(getVariable(), target));

        instructions.add(new NeutralInstruction(getVariable(), skip));

        return instructions;
    }


}
