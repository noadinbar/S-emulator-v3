package utils;

import structure.execution.ExecutionContext;
import structure.execution.FunctionExecutor;
import structure.execution.FunctionExecutorImpl;
import structure.function.Function;
import structure.instruction.synthetic.ArgKind;
import structure.instruction.synthetic.ArgVal;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.ArrayList;
import java.util.List;

public final class InstructionsHelpers {

    public InstructionsHelpers(){}

    public List<ArgVal> parseFunctionInputs(String argsString, ExecutionContext ctx) {
        List<ArgVal> out = new ArrayList<>();
        if (argsString == null) return out;

        List<String> tokens = splitTopArguments(argsString);

        for (String raw : tokens) {
            String t = raw.trim();
            if (t.isEmpty()) { out.add(ArgVal.ofLong(0L)); continue; }

            if (t.startsWith("(") && t.endsWith(")")) {
                out.add(ArgVal.ofString(t));
                continue;
            }

            try {
                out.add(ArgVal.ofLong(Long.parseLong(t)));
                continue;
            } catch (NumberFormatException ignore) {}

            if ("y".equals(t)) {
                out.add(ArgVal.ofLong(ctx.getVariableValue(Variable.RESULT)));
            } else if (t.length() >= 2 && (t.charAt(0) == 'x' || t.charAt(0) == 'z')) {
                try {
                    int idx = Integer.parseInt(t.substring(1));
                    VariableType vt = (t.charAt(0) == 'x') ? VariableType.INPUT : VariableType.WORK;
                    out.add(ArgVal.ofLong(ctx.getVariableValue(new VariableImpl(vt, idx))));
                } catch (Exception e) {
                    out.add(ArgVal.ofLong(0L));
                }
            } else {
                out.add(ArgVal.ofString(t));
            }
        }
        return out;
    }


    public List<String> splitTopArguments(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') { depth++; cur.append(c); continue; }
            if (c == ')') { depth--; cur.append(c); continue; }
            if (c == ',' && depth == 0) {
                out.add(cur.toString().trim());
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }

    public String removeOuterParentheses(String s) {
        if (s != null && s.length() >= 2 && s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')') {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    public int indexOfTopLevelComma(String s) {
        if (s == null) return -1;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) return i;
        }
        return -1;
    }

    public String[] splitFuncNameAndArgs(String expr) {
        String inner = removeOuterParentheses(expr);
        if (inner == null) return new String[] {"", ""};
        int cut = indexOfTopLevelComma(inner);
        String fname = (cut == -1) ? inner.trim() : inner.substring(0, cut).trim();
        String fargs = (cut == -1) ? "" : inner.substring(cut + 1).trim();
        return new String[] { fname, fargs };
    }
}
