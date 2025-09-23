package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Variable v     = getVariable();
        int k          = getConstant();
        Label target   = getTargetLabel();
        Label myLabel  = getMyLabel();
        Variable z     = prog.newWorkVar();
        Label notEqual = prog.newLabel();

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new AssignmentInstruction(z, v));
        else
            instructions.add(new AssignmentInstruction(z, v, myLabel));

        for (int i = 0; i < k; i++) {
            instructions.add(new JumpZeroInstruction(z, notEqual));
            instructions.add(new DecreaseInstruction(z));
        }

        instructions.add(new JumpNotZeroInstruction(z, notEqual));
        instructions.add(new GoToInstruction(z, target));
        instructions.add(new NeutralInstruction(v, notEqual));

        return instructions;
    }


}
