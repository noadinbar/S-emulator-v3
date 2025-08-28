package format;

import display.*;
import types.LabelDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InstructionFormatter {
    private static final int LABEL_WIDTH = 3;

    private InstructionFormatter(){}

    public static String formatDisplay(InstructionDTO ins) {
        String num   = String.format("#%d", ins.getNumber());
        String kind  = String.format("(%s)", ins.getKind()==InstrKindDTO.BASIC ? "B":"S");
        String label = String.format("[ %-" + LABEL_WIDTH + "s ]", formatLabel(ins.getLabel()));
        String body  = formatBody(ins.getBody());
        String cyc   = String.format("(%d)", ins.getCycles());
        return String.format("%s %s %s %s %s", num, kind, label, body, cyc);
    }

    public static String joinInputs(java.util.List<VarRefDTO> xs){
        if (xs == null || xs.isEmpty()) return "-";
        SortedSet<Integer> set = new TreeSet<>();
        for (VarRefDTO v : xs) {
            if (v != null && v.getVariable() == VarOptionsDTO.x) {
                set.add(v.getIndex());
            }
        }
        if (set.isEmpty()) return "-";
        StringJoiner sj = new StringJoiner(", ");
        for (int i : set) sj.add("x" + i);
        return sj.toString();
    }

    public static String joinLabels(java.util.List<LabelDTO> ls){
        if (ls == null || ls.isEmpty()) return "-";
        boolean hasExit = false;
        SortedSet<Integer> nums = new TreeSet<>();
        for (LabelDTO l : ls) {
            if (l == null) continue;
            if (l.isExit()) { hasExit = true; continue; }
            String name = l.getName();
            if (name == null || name.isBlank() || "EMPTY".equals(name)) continue;
            Integer n = parseLabelNumber(name); // "L12" -> 12
            if (n != null) nums.add(n);
        }
        if (nums.isEmpty() && !hasExit) return "-";
        StringJoiner sj = new StringJoiner(", ");
        for (int n : nums) sj.add("L" + n);
        if (hasExit) sj.add("EXIT");
        return sj.toString();
    }

    private static String formatBody(InstructionBodyDTO b){
        InstrOpDTO op = b.getOp();
        switch (op){
            case INCREASE:
                // "%s <- %s + 1"
                return String.format("%s <- %s + 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case DECREASE:
                // "%s <- %s - 1"
                return String.format("%s <- %s - 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case NEUTRAL:
                // "%s <- %s"
                return String.format("%s <- %s", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case ASSIGNMENT:
                // "%s <- %s"
                return String.format("%s <- %s", formatVar(b.getDest()), formatVar(b.getSource()));
            case CONSTANT_ASSIGNMENT:
                // "%s <- <const>"
                return String.format("%s <- %d", formatVar(b.getDest()), b.getConstant());
            case ZERO_VARIABLE:
                // "%s <- 0"
                return String.format("%s <- 0", formatVar(b.getDest()));
            case JUMP_NOT_ZERO:
                // "IF %s != 0 GOTO %s"
                return String.format("IF %s != 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_ZERO:
                // "IF %s = 0 GOTO %s"
                return String.format("IF %s = 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_CONSTANT:
                // "IF %s = %s GOTO %s"
                return String.format("IF %s = %d GOTO %s",
                        formatVar(b.getVariable()), b.getConstant(), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_VARIABLE:
                // "IF %s = %s GOTO %s"
                return String.format("IF %s = %s GOTO %s",
                        formatVar(b.getCompare()), formatVar(b.getCompareWith()), formatLabel(b.getJumpTo()));
            case GOTO_LABEL:
                // "GOTO %s"
                return String.format("GOTO %s", formatLabel(b.getJumpTo()));
            default:
                return "?";
        }
    }

    public static String formatExpanded(ExpandedInstructionDTO row) {
        String s = formatDisplay(row.getInstruction());
        var chain = row.getCreatedByChain();
        if (chain == null || chain.isEmpty()) return s;

        StringBuilder sb = new StringBuilder(s);
        for (InstructionDTO anc : chain) {
            sb.append("  >>>  ").append(formatDisplay(anc));
        }
        return sb.toString();
    }

    private static String formatLabel(LabelDTO l){
        if (l == null) return "";
        if (l.isExit()) return "EXIT";
        String n = l.getName();
        if (n == null || "EMPTY".equals(n) || n.isBlank()) return "";
        return n;
    }

    private static String formatVar(VarRefDTO v){
        if (v == null) return "";
        VarOptionsDTO s = v.getVariable();
        if (s == VarOptionsDTO.y) return "y";
        if (s == VarOptionsDTO.x) return "x" + v.getIndex();
        if (s == VarOptionsDTO.z) return "z" + v.getIndex();
        return "";
    }

    private static Integer parseLabelNumber(String label){
        if (label == null) return null;
        Matcher m = Pattern.compile("^L(\\d+)$").matcher(label.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
