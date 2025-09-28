package structure.execution;

import structure.function.Function;
import structure.program.Program;
import structure.variable.Variable;

import java.util.Map;

public interface FunctionExecutor {
    long run(Function function, Program sourceProgram, Long... inputs);
    int getLastRunCycles();
}
