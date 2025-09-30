package structure.execution;

import structure.variable.Variable;

import java.util.Map;

public interface ExecutionContext {

    long getVariableValue(Variable v);
    void updateVariable(Variable v, long value);
    Map<String, Long> snapshot();
}
