package structure.instruction;

import structure.execution.ExecutionContext;
import structure.expand.ExpansionManager;
import structure.label.Label;
import structure.variable.Variable;

import java.util.List;

public interface Instruction {

    String getName();
    Label execute(ExecutionContext context);
    List<Instruction> expand(ExpansionManager prog);
    int cycles();
    char kind();
    Label getMyLabel();
    Variable getVariable();
    int getDegree();
}
