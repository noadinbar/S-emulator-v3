package exportToDTO;

import api.DebugAPI;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import structure.execution.ExecutionContext;
import structure.execution.ExecutionContextImpl;
import structure.execution.ProgramExecutorImpl;
import structure.instruction.Instruction;
import structure.label.Label;
import structure.label.FixedLabel;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;
import types.VarOptionsDTO;
import types.VarRefDTO;

import java.util.*;

public class DebugAPIImpl implements DebugAPI {

    private final ProgramImpl expanded;
    private final ProgramImpl original;
    private final int degree;

    private List<Instruction> instructions;
    private Map<String,Integer> labelToIndex;
    private ExecutionContext context;
    private int pc;
    private long cyclesBeforeStep;
    private long logicalCyclesSoFar;
    private boolean terminated;
    private ProgramExecutorImpl runner;

    public DebugAPIImpl(ProgramImpl expanded, ProgramImpl original, int degree) {
        this.expanded = expanded;
        this.original = original;
        this.degree = Math.max(0, degree);
    }

    @Override
    public DebugStateDTO init(ExecutionRequestDTO req) {
        this.instructions = expanded.getInstructions();
        this.labelToIndex = buildLabelIndex(instructions);
        this.context = new ExecutionContextImpl();

        List<Long> inputs = (req == null || req.getInputs() == null)
                ? Collections.emptyList()
                : req.getInputs();

        for (int i = 0; i < inputs.size(); i++) {
            long val = (inputs.get(i) == null ? 0L : inputs.get(i));
            context.updateVariable(new VariableImpl(VariableType.INPUT, i + 1), val); // x(i+1) = val
        }
        context.updateVariable(Variable.RESULT, 0L); // y=0

        this.pc = 0;
        this.terminated = (instructions.isEmpty());
        this.runner = new ProgramExecutorImpl(expanded, original);
        this.cyclesBeforeStep = runner.getCycles();
        this.logicalCyclesSoFar = 0L;

        return new DebugStateDTO(
                degree,
                pc,
                logicalCyclesSoFar,
                snapshotVars(context),
                terminated
        );
    }

    @Override
    public DebugStepDTO step() {
        if (terminated || pc < 0 || pc >= instructions.size()) {
            return new DebugStepDTO(
                    Math.max(pc, 0),
                    0L,
                    new DebugStateDTO(degree, pc, logicalCyclesSoFar, snapshotVars(context), true),
                    List.of()
            );
        }
        int executedPc = pc;
        pc = runner.singleExecute(instructions, labelToIndex, context, pc);

        if (pc < 0 || pc >= instructions.size()) {
            terminated = true;
        }

        long cyclesAfter = runner.getCycles();
        long delta = cyclesAfter - cyclesBeforeStep;
        cyclesBeforeStep = cyclesAfter;
        logicalCyclesSoFar += delta;

        DebugStateDTO newState = new DebugStateDTO(
                degree,
                pc,
                logicalCyclesSoFar,
                snapshotVars(context),
                terminated
        );
        return new DebugStepDTO(executedPc, delta, newState, List.of());
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void restore(final DebugStateDTO snapshot) {
        ExecutionContext newCtx = new ExecutionContextImpl();
        for (VarValueDTO vv : snapshot.getVars()) {
            Variable var = toVariable(vv);
            newCtx.updateVariable(var, vv.getValue());
        }
        this.context = newCtx;
        this.pc = snapshot.getPc();
        this.terminated = (pc < 0 || pc >= instructions.size());
        this.logicalCyclesSoFar = snapshot.getCyclesSoFar();
        this.cyclesBeforeStep = runner.getCycles();
    }

    // ===== utils =====

    private static Variable toVariable(VarValueDTO vv) {
        VarOptionsDTO kind = vv.getVar().getVariable();
        int idx = vv.getVar().getIndex();
        if (kind == VarOptionsDTO.y) return Variable.RESULT;                 // y
        if (kind == VarOptionsDTO.x) return new VariableImpl(VariableType.INPUT, idx); // x_i
        /* kind == z */ return new VariableImpl(VariableType.WORK, idx);              // z_i
    }

    private static Map<String,Integer> buildLabelIndex(List<Instruction> instructions) {
        Map<String,Integer> map = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            Label lab = instructions.get(i).getMyLabel();
            if (lab != null && lab != FixedLabel.EMPTY) {
                String rep = lab.getLabelRepresentation();
                if (rep != null && !rep.isBlank()) {
                    map.putIfAbsent(rep.trim(), i);
                }
            }
        }
        return map;
    }

    private static List<VarValueDTO> snapshotVars(ExecutionContext context) {
        Map<String, Long> snap = (context).snapshot();
        List<VarValueDTO> out = new ArrayList<>();

        for (Map.Entry<String,Long> e : snap.entrySet()) {
            String name = e.getKey();
            long val = e.getValue();

            if ("y".equals(name)) {
                out.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.y, 0), val));
            } else if (name.startsWith("x")) {
                try {
                    int idx = Integer.parseInt(name.substring(1));
                    out.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.x, idx), val));
                } catch (NumberFormatException ignore) {}
            } else if (name.startsWith("z")) {
                try {
                    int idx = Integer.parseInt(name.substring(1));
                    out.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.z, idx), val));
                } catch (NumberFormatException ignore) {}
            }
        }

        out.sort(Comparator.<VarValueDTO>comparingInt(v ->
                v.getVar().getVariable() == VarOptionsDTO.y ? -1 :
                        v.getVar().getVariable() == VarOptionsDTO.x ? 0 : 1
        ).thenComparingInt(v -> v.getVar().getIndex()));

        return out;
    }
}
