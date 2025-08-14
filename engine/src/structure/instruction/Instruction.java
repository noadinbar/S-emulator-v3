package structure.instruction;

import structure.execution.ExecutionContext;
import structure.label.Label;
import structure.variable.Variable;

public interface Instruction {

    String getName();
    Label execute(ExecutionContext context);
    int cycles();
    Label getLabel();
    Variable getVariable();
}
