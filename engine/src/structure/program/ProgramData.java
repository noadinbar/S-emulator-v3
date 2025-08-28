package structure.program;

import structure.instruction.Instruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;
import structure.variable.VariableType;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public final class ProgramData {
    private final List<String> inputs; // x1, x2, ...
    private final List<String> labels; // L1, L2, ..., EXIT

    private ProgramData(List<String> inputs, List<String> labels) {
        this.inputs = List.copyOf(inputs);
        this.labels = List.copyOf(labels);
    }

    public static ProgramData build(structure.program.ProgramImpl program) {
        if (program == null || program.getInstructions() == null) {
            return new ProgramData(List.of(), List.of());
        }

        Set<String> inputSet = new HashSet<>();
        Set<String> labelSet = new HashSet<>();
        boolean hasExit = false;

        for (Instruction inst : program.getInstructions()) {
            addInputIfX(inputSet, inst.getVariable());
            hasExit |= addLabelIfNotEmpty(labelSet, inst.getMyLabel());
            hasExit |= collectViaReflection(inst, inputSet, labelSet);
        }

        List<String> inputs = new ArrayList<>(inputSet);
        inputs.sort(Comparator.comparingInt(ProgramData::xIndex));
        List<String> labels = new ArrayList<>(labelSet);
        labels.sort(Comparator.comparingInt(ProgramData::lIndex));
        if (hasExit) labels.add("EXIT");

        return new ProgramData(inputs, labels);
    }

    public List<String> getInputs() { return inputs; }
    public List<String> getLabels() { return labels; }


    private static boolean collectViaReflection(Instruction inst, Set<String> inputSet, Set<String> labelSet) {
        boolean sawExit = false;
        Method[] methods = inst.getClass().getMethods();
        for (Method m : methods) {
            if (m.getParameterCount() != 0) continue;
            Class<?> rt = m.getReturnType();

            if (!isVariableLikeReturn(rt) && !isLabelLikeReturn(rt)) continue;

            try {
                Object value = m.invoke(inst);
                sawExit |= addByType(value, inputSet, labelSet);
            } catch (Exception ignore) {
            }
        }
        return sawExit;
    }

    private static boolean addByType(Object val, Set<String> inputSet, Set<String> labelSet) {
        if (val == null) return false;

        if (val instanceof Variable v) {
            addInputIfX(inputSet, v);
            return false;
        }

        if (val instanceof Label lab) {
            return addLabelIfNotEmpty(labelSet, lab);
        }

        if (val instanceof Optional<?> opt) {
            return opt.map(o -> addByType(o, inputSet, labelSet)).orElse(false);
        }

        if (val instanceof Iterable<?> it) {
            boolean sawExit = false;
            for (Object o : it) {
                sawExit |= addByType(o, inputSet, labelSet);
            }
            return sawExit;
        }

        if (val.getClass().isArray()) {
            int len = Array.getLength(val);
            boolean sawExit = false;
            for (int i = 0; i < len; i++) {
                sawExit |= addByType(Array.get(val, i), inputSet, labelSet);
            }
            return sawExit;
        }

        return false;
    }

    private static boolean isVariableLikeReturn(Class<?> rt) {
        if (Variable.class.isAssignableFrom(rt)) return true;
        if (Optional.class.isAssignableFrom(rt)) return true;
        if (Iterable.class.isAssignableFrom(rt)) return true;
        return rt.isArray();
    }

    private static boolean isLabelLikeReturn(Class<?> rt) {
        if (Label.class.isAssignableFrom(rt)) return true;
        if (Optional.class.isAssignableFrom(rt)) return true;
        if (Iterable.class.isAssignableFrom(rt)) return true;
        return rt.isArray();
    }

    private static void addInputIfX(Set<String> set, Variable v) {
        if (v == null) return;
        if (v.getType() == VariableType.INPUT) {
            String rep = v.getRepresentation();
            if (rep != null && !rep.isBlank()) set.add(rep.trim().toLowerCase()); // x1,x2...
        }
    }

    private static boolean addLabelIfNotEmpty(Set<String> set, Label lab) {
        if (lab == null || lab == FixedLabel.EMPTY) return false;
        String s = lab.getLabelRepresentation();
        if (s == null || s.isBlank()) return false;
        String norm = s.trim().toUpperCase();
        if (norm.equals("EXIT")) return true;
        if (isL(norm)) set.add(norm);
        return false;
    }

    private static boolean isL(String s) {
        if (s.isEmpty() || s.charAt(0) != 'L' || s.length() == 1) return false;
        for (int i = 1; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    private static int xIndex(String x) {
        try { return Integer.parseInt(x.substring(1)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }
    private static int lIndex(String l) {
        try { return Integer.parseInt(l.substring(1)); } catch (Exception e) { return Integer.MAX_VALUE; }
    }
}
