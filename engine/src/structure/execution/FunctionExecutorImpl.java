package structure.execution;

import structure.function.Function;
import structure.instruction.Instruction;
import structure.program.Program;
import structure.program.ProgramImpl;
import structure.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class FunctionExecutorImpl implements FunctionExecutor {

    private int lastRunCycles = 0;
    private Map<Variable, Long> finals = new HashMap<>();

    @Override
    public long run(Function function, Program sourceProgram, Long... inputs) {
        ProgramImpl temp = new ProgramImpl("Function" + function.getName());
        for (Instruction ins : function.getInstructions()) {
            temp.addInstruction(ins);
        }

        if (sourceProgram != null) {
            for (Function fn : sourceProgram.getFunctions()) {
                temp.addFunction(fn);
            }
        }

        ProgramExecutorImpl exec = new ProgramExecutorImpl(temp, temp);
        long result = exec.run(inputs);
        this.lastRunCycles = exec.getCycles();
        this.finals = exec.variableState();
        return result;
    }

    @Override
    public int getLastRunCycles() { return lastRunCycles; }

    @Override
    public Map<Variable, Long> getFinalVariables() { return finals; }
}
