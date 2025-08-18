package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

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
    public String formatDisplay() {
        Label t = getTarget();
        String ts = (t == null) ? "" : t.getLabelRepresentation();
        return String.format("GOTO %s", ts);
    }
}
