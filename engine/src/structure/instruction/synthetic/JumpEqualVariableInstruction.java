package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Variable v       = getVariable();
        Variable v2      = getToCompare();
        Label   target   = getTargetLabel();
        Label   myLabel  = getMyLabel();

        Variable z1 = prog.newWorkVar();
        Variable z2 = prog.newWorkVar();
        Label L1 = prog.newLabel();
        Label L2 = prog.newLabel();
        Label L3 = prog.newLabel();

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new AssignmentInstruction(z1, v));
        else
            instructions.add(new AssignmentInstruction(z1, v, myLabel));

        // z2 <- V'
        instructions.add(new AssignmentInstruction(z2, v2));

        // L2: IF z1 == 0 GOTO L3
        instructions.add(new JumpZeroInstruction(z1, L3, L2));

        // IF z2 == 0 GOTO L1
        instructions.add(new JumpZeroInstruction(z2, L1));

        // z1 <- z1 - 1
        instructions.add(new DecreaseInstruction(z1));

        // z2 <- z2 - 1
        instructions.add(new DecreaseInstruction(z2));

        // GOTO L2
        instructions.add(new GoToInstruction(z2, L2));

        // L3: IF z2 == 0 GOTO L
        instructions.add(new JumpZeroInstruction(z2, target, L3));

        // L1: y <- y   (Neutral)
        instructions.add(new NeutralInstruction(v, L1));

        return instructions;
    }

}
