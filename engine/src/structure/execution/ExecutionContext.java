package structure.execution;

import structure.variable.Variable;

public interface ExecutionContext {

    long getVariableValue(Variable v);
    void updateVariable(Variable v, long value);
    java.util.Map<String, Long> snapshot();
}
