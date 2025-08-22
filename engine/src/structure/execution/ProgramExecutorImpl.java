package structure.execution;


import structure.instruction.Instruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.program.Program;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecutor{

    private final Program program;

    public ProgramExecutorImpl(Program program) {
        this.program = program;
    }

    public long run(Long... input) {

        // 0) שליפת ההוראות
        List<Instruction> instructions = program.getInstructions();
        //if (instructions == null || instructions.isEmpty()) {
          //  return 0L; // אין הוראות – y0 נשאר 0
        //}

        // 1) מיפוי תווית שמוגדרת על הוראה → אינדקס הוראה
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

        // 2) קונטקסט ריצה: קלטים x1,x2,... ואתחול y0=0
        ExecutionContext context = new ExecutionContextImpl();
        List<Long> inputsList = new ArrayList<>(input.length);

        for (int i = 0; i < input.length; i++) {
            long val = (input[i] == null ? 0L : input[i]);
            inputsList.add(val);
            context.updateVariable(new VariableImpl(VariableType.INPUT, i + 1), val); // x(i+1) = val
        }
        context.updateVariable(Variable.RESULT, 0L);


        // 3) לולאת pc עד EXIT או סוף רשימת ההוראות
        int pc = 0;
        int cycles = 0; // לעתיד (פקודה 5)

        while (pc >= 0 && pc < instructions.size()) {
            Instruction current = instructions.get(pc);

            Label next = current.execute(context);
            cycles += current.cycles();

            if (next == FixedLabel.EXIT) {
                break; // סיום על EXIT
            } else if (next == null || next == FixedLabel.EMPTY) {
                pc++; // מעבר רגיל לשורה הבאה
            } else {
                String rep = next.getLabelRepresentation();
                if (rep == null || rep.isBlank()) {
                    pc++; // אם אין ייצוג ברור—נמשיך רגיל
                } else {
                    Integer jumpTo = labelToIndex.get(rep.trim());
                    if (jumpTo == null) {
                        // תווית יעד לא נמצאה – נסיים (אפשר גם לזרוק חריגה אם זה עדיף)
                        break;
                    }
                    pc = jumpTo;
                }
            }
        }
        long y = context.getVariableValue(Variable.RESULT);

// 5) שמירת ההרצה להיסטוריה (פקודה 5)
        ((ProgramImpl) program).addRunHistory(inputsList, y, cycles);

// 6) החזרת התוצאה
        return y;
    }
    @Override
    public Map<Variable, Long> variableState() {
        return Map.of();
    }
}
