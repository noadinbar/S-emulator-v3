package structure.execution;

import structure.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContextImpl implements ExecutionContext {
    private final Map<String, Long> values = new HashMap<>();

    @Override
    public long getVariableValue(Variable v) {
        return values.getOrDefault(v.getRepresentation(), 0L);
    }

    @Override
    public void updateVariable(Variable v, long value) {
        values.put(v.getRepresentation(), value);
    }

    @Override
    public Map<String, Long> snapshot() {
        return new HashMap<>(values);
    }
}
