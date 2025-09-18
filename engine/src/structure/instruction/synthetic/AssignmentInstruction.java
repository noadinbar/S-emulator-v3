package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class AssignmentInstruction extends AbstractInstruction {

    private final Variable toAssign;

    public AssignmentInstruction(Variable dest, Variable source) {
        super(InstructionKind.SYNTHETIC, InstructionType.ASSIGNMENT, dest,2);
        this.toAssign = source;
    }

    public AssignmentInstruction(Variable dest, Variable source, Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.ASSIGNMENT, dest, myLabel,2);
        this.toAssign = source;
    }

    public Variable getToAssign() {
        return toAssign;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(toAssign);
        context.updateVariable(getVariable(), variableValue);

        return FixedLabel.EMPTY;
    }

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Variable v      = getVariable();
        Variable src    = getToAssign();
        Label   myLabel = getMyLabel();

        Variable z1 = prog.newWorkVar();
        Label L1 = prog.newLabel();
        Label L2 = prog.newLabel();
        Label L3 = prog.newLabel();

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new ZeroVariableInstruction(v));
        else
            instructions.add(new ZeroVariableInstruction(v, myLabel));

        // IF V' != 0 GOTO L1
        instructions.add(new JumpNotZeroInstruction(src, L1));

        // GOTO L3
        instructions.add(new GoToInstruction(src, L3));

        // L1: V' <- V' - 1
        instructions.add(new DecreaseInstruction(src, L1));
        //     z1 <- z1 + 1
        instructions.add(new IncreaseInstruction(z1));
        //     IF V' != 0 GOTO L1
        instructions.add(new JumpNotZeroInstruction(src, L1));

        // L2: z1 <- z1 - 1
        instructions.add(new DecreaseInstruction(z1, L2));
        //     V <- V + 1
        instructions.add(new IncreaseInstruction(v));
        //     V' <- V' + 1
        instructions.add(new IncreaseInstruction(src));
        //     IF z1 != 0 GOTO L2
        instructions.add(new JumpNotZeroInstruction(z1, L2));

        // L3: V <- V   (Neutral)
        instructions.add(new NeutralInstruction(v, L3));

        return instructions;
    }


}
