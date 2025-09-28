package application.format;

import execution.VarValueDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ExecutionFormatter {
    private ExecutionFormatter(){}

    public static String formatAllVars(List<VarValueDTO> finals) {
        String xs = finals.stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.x)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .map(ExecutionFormatter::formatVar)
                .collect(Collectors.joining(System.lineSeparator()));

        String zs = finals.stream()
                .filter(v -> v.getVar().getVariable() == VarOptionsDTO.z)
                .sorted(Comparator.comparingInt(v -> v.getVar().getIndex()))
                .map(ExecutionFormatter::formatVar)
                .collect(Collectors.joining(System.lineSeparator()));

        if (!xs.isEmpty() && !zs.isEmpty()) return xs + System.lineSeparator() + zs;
        if (!xs.isEmpty()) return xs;
        return zs;
    }

    private static String formatVar(VarValueDTO vv) {
        VarRefDTO r = vv.getVar();
        String name = (r.getVariable() == VarOptionsDTO.x) ? "x" + r.getIndex()
                : (r.getVariable() == VarOptionsDTO.z) ? "z" + r.getIndex()
                : "y";
        return String.format("%s = %d", name, vv.getValue());
    }

}
