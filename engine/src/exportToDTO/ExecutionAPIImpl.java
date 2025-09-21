package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.*;
import exceptions.InvalidInputException;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import structure.execution.ProgramExecutorImpl;
import structure.program.Program;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import types.VarOptionsDTO;
import types.VarRefDTO;

import java.util.*;

public class ExecutionAPIImpl implements ExecutionAPI {
    private final Program program;
    private final Program originalProgram;
    private final DisplayAPI display;

    // הבנאי הישן – עכשיו מפנה לחדש, לשמירת תאימות קריאות קיימות
    public ExecutionAPIImpl(ProgramImpl program,
                            ProgramImpl originalProgram) {
        this(program, originalProgram, null);
    }

    public ExecutionAPIImpl(ProgramImpl program,
                            ProgramImpl originalProgram,
                            DisplayAPI display) {
        this.program = program;
        this.originalProgram = originalProgram;
        this.display = display;
    }

    @Override
    public int getMaxDegree() {
        return ((ProgramImpl) program).calculateMaxDegree();
    }

    @Override
    public ExecutionDTO execute(ExecutionRequestDTO request) {
        DisplayDTO executed = DisplayMapper.toCommand2(program);

        SortedSet<Integer> xsInProgram = new TreeSet<>();
        SortedSet<Integer> zsInProgram = new TreeSet<>();
        collectXFromInputs(executed, xsInProgram);
        collectXZFromBody(executed, xsInProgram, zsInProgram);

        List<Long> inputsList = (request == null || request.getInputs() == null)
                ? Collections.emptyList()
                : request.getInputs();
        Long[] inputs = inputsList.toArray(new Long[0]);

        // checking if the inputs are not negative
        for (int i = 0; i < inputsList.size(); i++) {
            long inputAtIndex = inputsList.get(i) == null ? 0L : inputsList.get(i);
            if (inputAtIndex < 0) {
                int pos = i + 1;
                throw new InvalidInputException(
                        String.format("Inputs must be non-negative. You put x%d=%d.", pos, inputAtIndex)
                );
            }
        }

        ProgramExecutorImpl runner = new ProgramExecutorImpl(program, originalProgram);
        long y = runner.run(inputs);

        Map<Variable, Long> state = runner.variableState();

        List<VarValueDTO> finals = new ArrayList<>();
        finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.y, 0), y));

        for (int i : xsInProgram) {
            long val = state.getOrDefault(
                    new structure.variable.VariableImpl(structure.variable.VariableType.INPUT, i),
                    (i - 1 < inputsList.size()) ? inputsList.get(i - 1) : 0L
            );
            finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.x, i), val));
        }

        for (int i : zsInProgram) {
            long val = state.getOrDefault(
                    new structure.variable.VariableImpl(structure.variable.VariableType.WORK, i),
                    0L
            );
            finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.z, i), val));
        }

        long cycles = runner.getCycles();

        return new ExecutionDTO(y, cycles, finals, executed);
    }


    private static void collectXFromInputs(DisplayDTO dto, SortedSet<Integer> xs) {
        if (dto.getInputsInUse() == null) return;
        for (VarRefDTO v : dto.getInputsInUse()) {
            if (v != null && v.getVariable() == VarOptionsDTO.x) xs.add(v.getIndex());
        }
    }

    private static void collectXZFromBody(DisplayDTO dto, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (dto.getInstructions() == null) return;
        for (InstructionDTO ins : dto.getInstructions()) {
            InstructionBodyDTO b = ins.getBody();
            if (b == null) continue;
            addVar(b.getVariable(), xs, zs);
            addVar(b.getDest(), xs, zs);
            addVar(b.getSource(), xs, zs);
            addVar(b.getCompare(), xs, zs);
            addVar(b.getCompareWith(), xs, zs);
        }
    }

    private static void addVar(VarRefDTO v, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (v == null) return;
        if (v.getVariable() == VarOptionsDTO.x) xs.add(v.getIndex());
        else if (v.getVariable() == VarOptionsDTO.z) zs.add(v.getIndex());
    }
}
