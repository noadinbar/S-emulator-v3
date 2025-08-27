package exportToDTO;

import api.ExecutionAPI;
import display.Command2DTO;
import display.InstructionDTO;
import display.InstructionBodyDTO;
import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;
import execution.VarValueDTO;
import structure.execution.ProgramExecutorImpl;
import structure.program.Program;
import structure.program.ProgramImpl;
import types.VarOptionsDTO;
import types.VarRefDTO;

import java.util.*;

/**
 * פקודה 4 – משתמש ב-ProgramExecuterImpl.run(Long... input) לעדכון y.
 * כרגע: ללא הרחבה (degree לא בשימוש), מציג תוכנית AS-IS, cycles מחושב מ-ProgramImpl.
 * x_i בסיום נלקחים מהקלט (חסר→0), z_i=0 עד להרצה מלאה.
 */
public class ExecutionAPIImpl implements ExecutionAPI {
    private final Program program;

    public ExecutionAPIImpl(Program program) {
        this.program = program;
    }

    @Override
    public int getMaxDegree() {
        return ((ProgramImpl) program).calculateMaxDegree();
    }

    @Override
    public ExecutionDTO execute(ExecutionRequestDTO request) {
        // 0) התוכנית להצגה – כרגע AS IS
        Command2DTO executed = DisplayMapper.toCommand2(program);

        // 1) לאסוף אילו x/z מופיעים בתוכנית (כדי לדעת מה להציג בסוף)
        SortedSet<Integer> xsInProgram = new TreeSet<>();
        SortedSet<Integer> zsInProgram = new TreeSet<>();
        collectXFromInputs(executed, xsInProgram);
        collectXZFromBody(executed, xsInProgram, zsInProgram);

        // 2) הכנת קלטים ל-varargs של run(Long... input)
        List<Long> inputsList = (request == null || request.getInputs() == null)
                ? Collections.emptyList()
                : request.getInputs();
        Long[] inputs = inputsList.toArray(new Long[0]);

        // 3) להריץ Y אמיתי דרך ProgramExecuterImpl.run(...)
        //    התאימי את ה-import/שם החבילה למחלקה אצלך אם שונה:
        ProgramExecutorImpl runner = new ProgramExecutorImpl((ProgramImpl) program);
        long y = runner.run(inputs); // ← החתימה שנתת: public long run(Long... input)

        // 4) finals: y, כל x לפי הקלט, כל z = 0 (עד שמחזירים מצב סופי מה-runner)
        // מצב המשתנים האמיתי מהריצה
        java.util.Map<structure.variable.Variable, Long> state = runner.variableState();

        List<VarValueDTO> finals = new ArrayList<>();
        finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.y, 0), y));

// x_i מתוך מצב ההרצה (נפילה לקלט רק אם לא קיים במפה)
        for (int i : xsInProgram) {
            long val = state.getOrDefault(
                    new structure.variable.VariableImpl(structure.variable.VariableType.INPUT, i),
                    (i - 1 < inputsList.size()) ? inputsList.get(i - 1) : 0L
            );
            finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.x, i), val));
        }

// z_i מתוך מצב ההרצה (0 רק אם באמת לא עודכן)
        for (int i : zsInProgram) {
            long val = state.getOrDefault(
                    new structure.variable.VariableImpl(structure.variable.VariableType.WORK, i),
                    0L
            );
            finals.add(new VarValueDTO(new VarRefDTO(VarOptionsDTO.z, i), val));
        }


        // 5) cycles אמיתי מ-ProgramImpl (אם ל-runner יש getCycles – אפשר להחליף לזה)
        long cycles = ((ProgramImpl) program).calculateCycles();

        return new ExecutionDTO(y, cycles, finals, executed);
    }

    // ===== helpers (כמו קודם) =====

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
