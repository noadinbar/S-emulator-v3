package structure.execution;

import structure.instruction.Instruction;
import structure.variable.Variable;

import java.util.List;
import java.util.Map;

public interface ProgramExecutor {

    long run(Long... input);
    public int singleExecute(List<Instruction> instructions,
                             Map<String, Integer> labelToIndex,
                             ExecutionContext context,
                             int pc);
    Map<Variable, Long> variableState();
}
