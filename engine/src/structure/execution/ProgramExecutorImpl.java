package structure.execution;


import structure.instruction.Instruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.program.Program;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.*;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final Program program;
    private final Program originalProgram;
    private Map<Variable, Long> lastState = new HashMap<>();
    private int cycles = 0;


    public ProgramExecutorImpl(Program program, Program originalProgram) {
        this.program = program;
        this.originalProgram = originalProgram;
    }

    public long run(Long... input) {
        List<Instruction> instructions = program.getInstructions();
        Map<String, Integer> labelToIndex = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Label lab = instructions.get(i).getMyLabel();
            if (lab != null && lab != FixedLabel.EMPTY) {
                String rep = lab.getLabelRepresentation();
                if (rep != null && !rep.isBlank()) {
                    labelToIndex.putIfAbsent(rep.trim(), i);
                }
            }
        }

        ExecutionContext context = new ExecutionContextImpl();
        List<Long> inputsList = new ArrayList<>(input.length);

        for (int i = 0; i < input.length; i++) {
            long val = (input[i] == null ? 0L : input[i]);
            inputsList.add(val);
            context.updateVariable(new VariableImpl(VariableType.INPUT, i + 1), val); // x(i+1) = val
        }
        context.updateVariable(Variable.RESULT, 0L);

        int pc = 0;
        while (pc >= 0 && pc < instructions.size()) {
            Instruction current = instructions.get(pc);

            Label next = current.execute(context);

            cycles += current.cycles();

            if (next == FixedLabel.EXIT) {
                break;
            } else if (next == FixedLabel.EMPTY) {
                pc++;
            } else {
                String rep = next.getLabelRepresentation();
                if (rep.isBlank()) {
                    pc++;
                } else {
                    Integer jumpTo = labelToIndex.get(rep.trim());
                    if (jumpTo == null) {
                        break;
                    }
                    pc = jumpTo;
                }
            }
        }
        long y = context.getVariableValue(Variable.RESULT);

        Map<String, Long> snap = ((ExecutionContextImpl) context).snapshot();
        Map<Variable, Long> state = new HashMap<>();
        for (Map.Entry<String, Long> e : snap.entrySet()) {
            String name = e.getKey();
            long val = e.getValue();

            if ("y".equals(name)) {
                state.put(Variable.RESULT, val);
            } else if (name.startsWith("x")) {
                try {
                    int idx = Integer.parseInt(name.substring(1));
                    state.put(new VariableImpl(VariableType.INPUT, idx), val);
                } catch (NumberFormatException ignore) {}
            } else if (name.startsWith("z")) {
                try {
                    int idx = Integer.parseInt(name.substring(1));
                    state.put(new VariableImpl(VariableType.WORK, idx), val);
                } catch (NumberFormatException ignore) {}
            }
        }
        this.lastState = state;

        ((ProgramImpl) originalProgram).addRunHistory(inputsList, y, cycles);

        return y;
    }

    public int getCycles() {
        return cycles;
    }

    @Override
    public Map<Variable, Long> variableState() {
        return Collections.unmodifiableMap(lastState);
    }
}
