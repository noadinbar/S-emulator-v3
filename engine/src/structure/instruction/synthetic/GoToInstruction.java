package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class GoToInstruction extends AbstractInstruction {

    private final Label target;

    public GoToInstruction(Variable variable, Label targetLabel) {
        this(variable, targetLabel, FixedLabel.EMPTY);
    }

    public GoToInstruction(Variable variable, Label targetLabel, Label label) {
        super(InstructionKind.SYNTHETIC, InstructionType.GOTO_LABEL, variable, label);
        this.target = targetLabel;
    }
    public Label getTarget() {
        return target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return target;
    }

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        List<Instruction> instructions = new ArrayList<>();

        Label myLabel = getMyLabel();
        Variable work  = prog.newWorkVar();          // z#

        if (myLabel == FixedLabel.EMPTY)
            instructions.add(new IncreaseInstruction(work));
        else
            instructions.add(new IncreaseInstruction(work, myLabel));

        instructions.add(new JumpNotZeroInstruction(work, getTarget()));

        return instructions;
    }


}
