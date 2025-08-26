package structure.expand;

import structure.label.Label;
import structure.label.LabelImpl;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

public class ExpansionManagerImpl implements ExpansionManager {

    private int nextLabelIndex;
    private int nextWorkVarIndex;

    public ExpansionManagerImpl(int nextLabelIndex, int nextWorkVarIndex) {
        this.nextLabelIndex = Math.max(1, nextLabelIndex);
        this.nextWorkVarIndex = Math.max(1, nextWorkVarIndex);
    }

    @Override
    public Label newLabel() {
        Label created = new LabelImpl("L" + nextLabelIndex);
        nextLabelIndex++;
        return created;
    }

    @Override
    public Variable newWorkVar() {
        Variable created = new VariableImpl(VariableType.WORK, nextWorkVarIndex);
        nextWorkVarIndex++;
        return created;
    }
}