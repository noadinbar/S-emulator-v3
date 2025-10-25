package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.execution.FunctionExecutor;
import structure.execution.FunctionExecutorImpl;
import structure.expand.ExpansionManager;
import structure.function.Function;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.label.LabelImpl;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;
import utils.InstructionsHelpers;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuotationInstruction extends AbstractInstruction {

    private final String functionName;
    private final String userString;
    private final String functionArguments;
    private int lastFunctionCycles = 0;
    private static InstructionsHelpers helper = new InstructionsHelpers();

    public QuotationInstruction(Variable dest,
                                String functionName,
                                String userString,
                                String functionArguments) {
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest);
        this.functionName = functionName;
        this.userString = userString;
        if(functionArguments.startsWith("(") && functionArguments.endsWith(")"))
        {

        }

        this.functionArguments = functionArguments;
    }

    public QuotationInstruction(Variable dest,
                                String functionName,
                                String userString,
                                String functionArguments,
                                Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest, myLabel);
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    @Override
    public int cycles() {
        return InstructionType.QUOTE.getCycles() + lastFunctionCycles;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getFunctionArguments() {
        return functionArguments;
    }

    public String getUserString() {
        return userString;
    }

    public Label execute(ExecutionContext context, Program program) {
        Function function;
        function = program.getFunction(functionName);
        final int[] nestedCycles = new int[]{0};

        List<ArgVal> inputsList = helper.parseFunctionInputs(functionArguments, context);
        Long[] inputs = new Long[inputsList.size()];
        for (int i = 0; i < inputsList.size(); i++) {
            inputs[i] = evalArgVal(inputsList.get(i), context, program);
        }

        FunctionExecutor functionRunner = new FunctionExecutorImpl();
        long result = functionRunner.run(function, program, inputs);
        nestedCycles[0] += functionRunner.getLastRunCycles();
        context.updateVariable(getVariable(), result);
        lastFunctionCycles = nestedCycles[0] + functionRunner.getLastRunCycles();
        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }

// QuotationInstruction.java — expand(...) + helpers (consolidated)

    public List<Instruction> expand(ExpansionManager prog, Program program) {
        Function function = program.getFunction(functionName);
        List<Instruction> newInstructions = new ArrayList<>();

        SortedSet<Integer> xs = new TreeSet<>();
        SortedSet<Integer> zs = new TreeSet<>();
        Set<Label> labelSet = new LinkedHashSet<>();
        Variable y = Variable.RESULT;

        Map<String, Variable> argToX = buildArgToXMap(functionArguments);
        Map<Variable, Variable> convertToZ = new LinkedHashMap<>();
        Map<Label, Label> newLabelMap = new LinkedHashMap<>();

        // ensure zTmp for every x# in QUOTE's argument list
        for (Variable xVar : argToX.values()) {
            convertToZ.putIfAbsent(xVar, prog.newWorkVar());
        }

        // pass 1: collect vars/labels referenced directly in function body
        for (Instruction ins : function.getInstructions()) {
            addLabel(ins.getMyLabel(), labelSet);

            switch (ins.getName()) {
                case "INCREASE":
                case "DECREASE":
                case "NEUTRAL":
                    addVar(ins.getVariable(), xs, zs);
                    break;

                case "ASSIGNMENT": {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    addVar(a.getVariable(), xs, zs);
                    addVar(a.getToAssign(), xs, zs);
                    break;
                }

                case "CONSTANT_ASSIGNMENT": {
                    ConstantAssignmentInstruction c = (ConstantAssignmentInstruction) ins;
                    addVar(c.getVariable(), xs, zs);
                    break;
                }

                case "JUMP_NOT_ZERO": {
                    JumpNotZeroInstruction j = (JumpNotZeroInstruction) ins;
                    addVar(j.getVariable(), xs, zs);
                    addLabel(j.getTargetLabel(), labelSet);
                    break;
                }

                case "JUMP_ZERO": {
                    JumpZeroInstruction jz = (JumpZeroInstruction) ins;
                    addVar(jz.getVariable(), xs, zs);
                    addLabel(jz.getTargetLabel(), labelSet);
                    break;
                }

                case "JUMP_EQUAL_CONSTANT": {
                    JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) ins;
                    addVar(jec.getVariable(), xs, zs);
                    addLabel(jec.getTargetLabel(), labelSet);
                    break;
                }

                case "JUMP_EQUAL_VARIABLE": {
                    JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) ins;
                    addVar(jev.getVariable(), xs, zs);
                    addVar(jev.getToCompare(), xs, zs);
                    addLabel(jev.getTargetLabel(), labelSet);
                    break;
                }

                case "GOTO_LABEL": {
                    GoToInstruction go = (GoToInstruction) ins;
                    addVar(go.getVariable(), xs, zs);
                    addLabel(go.getTarget(), labelSet);
                    break;
                }

                case "JUMP_EQUAL_FUNCTION": {
                    JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) ins;
                    addVar(jef.getVariable(), xs, zs);
                    addLabel(jef.getTargetLabel(), labelSet);
                    break;
                }
            }
        }

        // pass 1.5: also collect x#/z# that appear only inside functionArguments of QUOTE/JEF in the body
        for (Instruction ins : function.getInstructions()) {
            switch (ins.getName()) {
                case "QUOTE": {
                    QuotationInstruction q = (QuotationInstruction) ins;
                    addVarsFromArgs(q.getFunctionArguments(), xs, zs);
                    break;
                }
                case "JUMP_EQUAL_FUNCTION": {
                    JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) ins;
                    addVarsFromArgs(jef.getFunctionArguments(), xs, zs);
                    break;
                }
            }
        }

        // allocate work-vars for all referenced x#/z#
        for (int index : xs) {
            convertToZ.putIfAbsent(new VariableImpl(VariableType.INPUT, index), prog.newWorkVar());
        }
        for (int index : zs) {
            convertToZ.putIfAbsent(new VariableImpl(VariableType.WORK, index), prog.newWorkVar());
        }

        // labels mapping (EXIT consistent)
        boolean hasExit = false;
        Label exitMapped = null;
        for (Label lbl : labelSet) {
            boolean isExit =
                    (lbl == FixedLabel.EXIT) || "EXIT".equals(lbl.getLabelRepresentation());
            if (isExit) {
                hasExit = true;
                if (exitMapped == null) exitMapped = prog.newLabel();
                newLabelMap.put(lbl, exitMapped);
            } else {
                newLabelMap.put(lbl, prog.newLabel());
            }
        }
        if (hasExit) {
            newLabelMap.putIfAbsent(FixedLabel.EXIT, exitMapped);
            newLabelMap.putIfAbsent(new LabelImpl("EXIT"), exitMapped);
        }

        // anchor original QUOTE label
        newInstructions.add(new NeutralInstruction(getVariable(), getMyLabel()));

        // PROLOG: evaluate each QUOTE argument into its x#-dest zTmp
        for (Map.Entry<String, Variable> e : argToX.entrySet()) {
            String t = e.getKey().trim();
            Variable xVar = e.getValue();
            Variable dest = convertToZ.get(xVar);

            Integer constant = tryParseInt(t);
            if (constant != null) {
                newInstructions.add(new ConstantAssignmentInstruction(dest, constant));
                continue;
            }

            Variable src = parseVariableToken(t); // y / x# / z#
            if (src != null) {
                newInstructions.add(new AssignmentInstruction(dest, src));
                continue;
            }

            String[] fa = helper.splitFuncNameAndArgs(t);
            String funcName = fa[0];
            String funcArgs = fa[1];
            String funcUserString = program.getFunction(funcName).getUserString();
            newInstructions.add(new QuotationInstruction(dest, funcName, funcUserString, funcArgs));
        }

        // ensure mapping for function result 'y'
        convertToZ.putIfAbsent(y, prog.newWorkVar());
        final Label exitTarget = exitMapped;

        // safe label mapping (covers EXIT in both representations)
        java.util.function.Function<Label, Label> mapTarget = (orig) -> {
            Label mapped = newLabelMap.get(orig);
            if (mapped != null) return mapped;
            if (orig == FixedLabel.EXIT || "EXIT".equals(orig.getLabelRepresentation())) {
                return (exitTarget != null) ? exitTarget : orig;
            }
            return orig;
        };

        // BODY: clone with var/label remap; also remap functionArguments for QUOTE/JEF
        for (Instruction ins : function.getInstructions()) {
            Label myLabel = (ins.getMyLabel() == FixedLabel.EMPTY)
                    ? FixedLabel.EMPTY
                    : mapTarget.apply(ins.getMyLabel());

            InstructionType type = InstructionType.valueOf(ins.getName().toUpperCase());

            switch (type) {
                case INCREASE: {
                    Variable v = convertToZ.get(ins.getVariable());
                    newInstructions.add(new IncreaseInstruction(v, myLabel));
                    break;
                }
                case DECREASE: {
                    Variable v = convertToZ.get(ins.getVariable());
                    newInstructions.add(new DecreaseInstruction(v, myLabel));
                    break;
                }
                case NEUTRAL: {
                    Variable v = convertToZ.get(ins.getVariable());
                    newInstructions.add(new NeutralInstruction(v, myLabel));
                    break;
                }
                case ZERO_VARIABLE: {
                    Variable v = convertToZ.get(ins.getVariable());
                    newInstructions.add(new ZeroVariableInstruction(v, myLabel));
                    break;
                }
                case ASSIGNMENT: {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    Variable dest = convertToZ.get(a.getVariable());
                    Variable src  = convertToZ.get(a.getToAssign());
                    newInstructions.add(new AssignmentInstruction(dest, src, myLabel));
                    break;
                }
                case CONSTANT_ASSIGNMENT: {
                    ConstantAssignmentInstruction c = (ConstantAssignmentInstruction) ins;
                    Variable dest = convertToZ.get(c.getVariable());
                    int k = c.getConstant();
                    newInstructions.add(new ConstantAssignmentInstruction(dest, k, myLabel));
                    break;
                }
                case JUMP_NOT_ZERO: {
                    JumpNotZeroInstruction jnz = (JumpNotZeroInstruction) ins;
                    Variable v = convertToZ.get(jnz.getVariable());
                    Label target = mapTarget.apply(jnz.getTargetLabel());
                    newInstructions.add(new JumpNotZeroInstruction(v, target, myLabel));
                    break;
                }
                case JUMP_ZERO: {
                    JumpZeroInstruction jz = (JumpZeroInstruction) ins;
                    Variable v = convertToZ.get(jz.getVariable());
                    Label target = mapTarget.apply(jz.getTargetLabel());
                    newInstructions.add(new JumpZeroInstruction(v, target, myLabel));
                    break;
                }
                case JUMP_EQUAL_CONSTANT: {
                    JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) ins;
                    Variable v = convertToZ.get(jec.getVariable());
                    Label target = mapTarget.apply(jec.getTargetLabel());
                    int k = jec.getConstant();
                    newInstructions.add(new JumpEqualConstantInstruction(v, target, k, myLabel));
                    break;
                }
                case JUMP_EQUAL_VARIABLE: {
                    JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) ins;
                    Variable v1 = convertToZ.get(jev.getVariable());
                    Variable v2 = convertToZ.get(jev.getToCompare());
                    Label target = mapTarget.apply(jev.getTargetLabel());
                    newInstructions.add(new JumpEqualVariableInstruction(v1, target, v2, myLabel));
                    break;
                }
                case GOTO_LABEL: {
                    GoToInstruction go = (GoToInstruction) ins;
                    Variable v = convertToZ.get(go.getVariable());
                    Label target = mapTarget.apply(go.getTarget());
                    newInstructions.add(new GoToInstruction(v, target, myLabel));
                    break;
                }
                case JUMP_EQUAL_FUNCTION: {
                    JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) ins;
                    Variable v = convertToZ.get(jef.getVariable());
                    Label target = mapTarget.apply(jef.getTargetLabel());
                    String fname = jef.getFunctionName();
                    String ustr  = jef.getUserString();
                    String fargs = remapFunctionArguments(jef.getFunctionArguments(), convertToZ);
                    newInstructions.add(new JumpEqualFunctionInstruction(v, target, fname, ustr, fargs, myLabel));
                    break;
                }
                case QUOTE: {
                    QuotationInstruction q = (QuotationInstruction) ins;
                    Variable dest = convertToZ.get(q.getVariable());
                    String fname = q.getFunctionName();
                    String ustr  = q.getUserString();
                    String fargs = remapFunctionArguments(q.getFunctionArguments(), convertToZ);
                    newInstructions.add(new QuotationInstruction(dest, fname, ustr, fargs, myLabel));
                    break;
                }
                default:
            }
        }

        // copy function result 'y' into original QUOTE destination (bind to EXIT if present)
        if (exitMapped != null) {
            newInstructions.add(new AssignmentInstruction(getVariable(), convertToZ.get(Variable.RESULT), exitMapped));
        } else {
            newInstructions.add(new AssignmentInstruction(getVariable(), convertToZ.get(Variable.RESULT)));
        }

        return newInstructions;
    }

    /* ===== helpers (same class) ===== */

    private static final Pattern X_IN_ARGS = Pattern.compile("\\bx(\\d+)\\b");
    private static final Pattern Z_IN_ARGS = Pattern.compile("\\bz(\\d+)\\b");

    private static void addVarsFromArgs(String args, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (args == null || args.isBlank()) return;
        Matcher mx = X_IN_ARGS.matcher(args);
        while (mx.find()) {
            try { xs.add(Integer.parseInt(mx.group(1))); } catch (Exception ignore) {}
        }
        Matcher mz = Z_IN_ARGS.matcher(args);
        while (mz.find()) {
            try { zs.add(Integer.parseInt(mz.group(1))); } catch (Exception ignore) {}
        }
    }

    private String remapFunctionArguments(String args, Map<Variable, Variable> map) {
        if (args == null || args.isBlank()) return args;
        StringBuilder out = new StringBuilder(args.length());
        int i = 0, n = args.length();
        while (i < n) {
            char c = args.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                int j = i + 1;
                while (j < n) {
                    char cj = args.charAt(j);
                    if (Character.isLetterOrDigit(cj) || cj == '_') j++; else break;
                }
                String tok = args.substring(i, j);
                out.append(remapToken(tok, map));
                i = j;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private String remapToken(String tok, Map<Variable, Variable> map) {
        if ("y".equals(tok)) {
            Variable v = map.get(Variable.RESULT);
            if (v instanceof VariableImpl) return v.getRepresentation();
            return tok;
        }
        if (tok.length() >= 2 && tok.charAt(0) == 'x') {
            try {
                int idx = Integer.parseInt(tok.substring(1));
                Variable v = map.get(new VariableImpl(VariableType.INPUT, idx));
                if (v instanceof VariableImpl) return v.getRepresentation();
                return tok;
            } catch (NumberFormatException ignore) { return tok; }
        }
        if (tok.length() >= 2 && tok.charAt(0) == 'z') {
            try {
                int idx = Integer.parseInt(tok.substring(1));
                Variable v = map.get(new VariableImpl(VariableType.WORK, idx));
                if (v instanceof VariableImpl) return v.getRepresentation();
                return tok;
            } catch (NumberFormatException ignore) { return tok; }
        }
        return tok; // function names / CONST0 / NOT / etc.
    }


    private Long evalArgVal(ArgVal a, ExecutionContext ctx, Program program) {
        if (a.getKind() == ArgKind.LONG) {
            return a.getLongValue();
        }
        String t = a.getText();

        if (t.startsWith("(") && t.endsWith(")")) {
            String[] fa = helper.splitFuncNameAndArgs(t);
            String fname = fa[0];
            String fargs = fa[1];

            try {
                Function f = program.getFunction(fname);
                List<ArgVal> nested = helper.parseFunctionInputs(fargs, ctx);

                Long[] nestedInputs = new Long[nested.size()];
                for (int i = 0; i < nested.size(); i++) {
                    nestedInputs[i] = evalArgVal(nested.get(i), ctx, program);
                }

                FunctionExecutor runner = new FunctionExecutorImpl();
                long val = runner.run(f, program, nestedInputs);
                lastFunctionCycles += runner.getLastRunCycles();

                return val;
            } catch (Exception ignore) {
                return 0L;
            }
        }
        return 0L;
    }

    private void addVar(Variable v, SortedSet<Integer> xs, SortedSet<Integer> zs) {
        if (v == null || v == Variable.RESULT) return;
        VariableType t = v.getType();
        if (t == VariableType.INPUT) {
            xs.add(v.getNumber());
        } else if (t == VariableType.WORK) {
            zs.add(v.getNumber());
        }
    }

    private void addLabel(Label lbl, Set<Label> labels) {
        if (lbl == null) return;
        if (lbl == FixedLabel.EMPTY) return;
        labels.add(lbl);
    }

    private Map<String, Variable> buildArgToXMap(String functionArguments) {
        Map<String, Variable> map = new LinkedHashMap<>();
        if (functionArguments == null || functionArguments.isBlank()) return map;

        List<String> items = helper.splitTopArguments(functionArguments);
        int idx = 1;
        for (String key : items) {
            String k = (key == null) ? "" : key.trim();
            if (!k.isEmpty()) map.putIfAbsent(k, new VariableImpl(VariableType.INPUT, idx));
            idx++;
        }
        return map;
    }

    private Integer tryParseInt(String s) {
        try { return Integer.valueOf(s); }
        catch (NumberFormatException ignore) { return null; }
    }

    private Variable parseVariableToken(String t) {
        if ("y".equals(t)) return Variable.RESULT;
        if (t.length() >= 2) {
            char kind = t.charAt(0);
            if (kind == 'x' || kind == 'z') {
                int idx = Integer.parseInt(t.substring(1));
                return (kind == 'x')
                        ? new VariableImpl(VariableType.INPUT, idx)
                        : new VariableImpl(VariableType.WORK, idx);
            }
        }
        return null;
    }

}
