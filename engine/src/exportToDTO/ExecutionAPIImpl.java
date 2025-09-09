package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.*;
import exceptions.InvalidInputException;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import execution.debug.DebugStateDTO;
import structure.execution.ProgramExecutorImpl;
import structure.program.Program;
import structure.program.ProgramImpl;
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

    // בנאי חדש עם DisplayAPI — נשתמש בו בהמשך לדיבאג/expand
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
        Command2DTO executed = DisplayMapper.toCommand2(program);

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

        Map<structure.variable.Variable, Long> state = runner.variableState();

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

    @Override
    public DebugStateDTO debugInit(ExecutionRequestDTO req) {
        // חייבים DisplayAPI מוזרק (באמצעות הבנאי שעשינו קודם)
        if (display == null) {
            throw new IllegalStateException("DisplayAPI not injected into ExecutionAPIImpl");
        }

        // הדרגה שעליה נרוץ בדיבאג
        final int degree = req.getDegree();

        // מרחיבים לדרגה הזו כדי לקבוע את ה-PC ההתחלתי (מס' ההוראה הראשון)
        final Command3DTO c3 = display.expand(degree);
        final java.util.List<ExpandedInstructionDTO> expanded =
                (c3 != null) ? c3.getInstructions() : null;

        final boolean noCode = (expanded == null || expanded.isEmpty());
        final int pc = noCode ? -1 : expanded.get(0).getInstruction().getNumber();
        final boolean terminated = noCode; // אם אין קוד — כבר "סיים"

        // צילום מצב משתנים התחלתי:
        // y = 0, וכל x_i מתוך הקלט (NULL -> 0). Z לא נדרש בשלב זה.
        final java.util.List<Long> inputs = req.getInputs();
        final java.util.List<VarValueDTO> varsSnapshot = new java.util.ArrayList<>();

        // y קודם
        varsSnapshot.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.y, 0), 0L));

        // x1, x2, ... לפי הקלט
        if (inputs != null) {
            for (int i = 0; i < inputs.size(); i++) {
                long val = (inputs.get(i) == null) ? 0L : inputs.get(i);
                varsSnapshot.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.x, i + 1), val));
            }
        }

        // cyclesSoFar = 0 במצב התחלתי
        return new DebugStateDTO(degree, pc, 0L, varsSnapshot, terminated);
    }


    private static void collectXFromInputs(Command2DTO dto, SortedSet<Integer> xs) {
        if (dto.getInputsInUse() == null) return;
        for (VarRefDTO v : dto.getInputsInUse()) {
            if (v != null && v.getVariable() == VarOptionsDTO.x) xs.add(v.getIndex());
        }
    }

    private static void collectXZFromBody(Command2DTO dto, SortedSet<Integer> xs, SortedSet<Integer> zs) {
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
