package format;

import execution.ExecutionDTO;
import execution.VarValueDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ExecutionFormatter {
    private ExecutionFormatter(){}

    // --- שלב 1: הצגת דרגה מקסימלית + אישור הדרגה שנבחרה ---
    public static String formatMaxDegree(int maxDeg) {
        return String.format("Max expansion degree: %d", maxDeg);
    }
    public static String confirmDegree(int degree) {
        String suffix = (degree == 0) ? " (AS IS)" : "";
        return String.format("Degree chosen: %d%s", degree, suffix);
    }

    // --- שלב 2: Inputs in use (ממחזר את ה-join שכבר כתבת) ---
    public static String formatInputsInUse(List<VarRefDTO> xs) {
        return String.format("Inputs in use: %s", InstructionFormatter.joinInputs(xs));
    }

    // --- שלבים 5–7: פלטים סופיים ---
    public static String formatY(ExecutionDTO dto) {
        return String.format("y = %d", dto.getyValue());
    }

    /** מדפיס את כל המשתנים בסיום: קודם כל ה-x לפי אינדקס, אחריהם ה-z לפי אינדקס */
    public static String formatAllVars(List<VarValueDTO> finals) {
        String xs = finals.stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.x)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .map(ExecutionFormatter::fmtVar)
                .collect(Collectors.joining(System.lineSeparator()));

        String zs = finals.stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.z)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .map(ExecutionFormatter::fmtVar)
                .collect(Collectors.joining(System.lineSeparator()));

        if (!xs.isEmpty() && !zs.isEmpty()) return xs + System.lineSeparator() + zs;
        if (!xs.isEmpty()) return xs;
        return zs; // ייתכן שאין z בכלל
    }

    public static String formatCycles(ExecutionDTO dto) {
        return String.format("cycles = %d", dto.getTotalCycles());
    }

    private static String fmtVar(VarValueDTO vv) {
        VarRefDTO r = vv.getVar();
        String name = (r.getVariable() == VarOptionsDTO.x) ? "x" + r.getIndex()
                : (r.getVariable() == VarOptionsDTO.z) ? "z" + r.getIndex()
                : "y";
        return String.format("%s = %d", name, vv.getValue());
    }
}
