package structure.expand;

import structure.label.Label;
import structure.variable.Variable;

public interface ExpansionManager {
    Label newLabel();
    Variable newWorkVar();
}