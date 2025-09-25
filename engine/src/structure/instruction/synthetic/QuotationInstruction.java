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
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.*;

import static java.lang.Integer.parseInt;

public class QuotationInstruction extends AbstractInstruction {

    private final String functionName;
    private final String userString;
    private final String functionArguments;
    private int lastFunctionCycles = 0;

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
        if (program == null) {
            context.updateVariable(getVariable(), 0L);
            lastFunctionCycles = 0;
            return FixedLabel.EMPTY;
        }

        Function function;
        function = program.getFunction(functionName);
        final int[] nestedCycles = new int[]{0};


        List<ArgVal> inputsList = parseFunctionInputs(functionArguments, context);
        Long[] inputs = new Long[inputsList.size()];
        for (int i = 0; i < inputsList.size(); i++) {
            inputs[i] = evalArgVal(inputsList.get(i), context, program);  // חדש
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

        boolean isExit=false;

        for (Variable xVar : argToX.values()) {
            if(!convertToZ.containsKey(xVar))
            {
                convertToZ.put(xVar, prog.newWorkVar());
            }
        }

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

                case "JUMP_EQUAL_FUNCTION": { //TODO: check after implement that arguments maybe need to be changed?
                    JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) ins;
                    addVar(jef.getVariable(), xs, zs);
                    addLabel(jef.getTargetLabel(), labelSet);
                    break;
                }

                case "QUOTE":
                    // ציטוט מקונן – כרגע לא מוסיפים עוד משתנים/תוויות מעבר ל-myLabel שכבר נאסףTODO:
                    break;

                default:
            }
        }

        for (int index : xs) {
            if(!convertToZ.containsKey(new VariableImpl(VariableType.INPUT, index))) {
                convertToZ.put(new VariableImpl(VariableType.INPUT, index), prog.newWorkVar());
            }
        }

        for (int index : zs) {
            if(!convertToZ.containsKey(new VariableImpl(VariableType.WORK, index))) {
                convertToZ.put(new VariableImpl(VariableType.WORK, index), prog.newWorkVar());
            }
        }

        for (Label lbl : labelSet) {
            if(!Objects.equals(lbl.getLabelRepresentation(), "EXIT")) {
                newLabelMap.put(lbl, prog.newLabel());
            }
            else isExit=true;
        }

        if (isExit) {newLabelMap.put(FixedLabel.EXIT,prog.newLabel());}


        newInstructions.add(new NeutralInstruction(getVariable(),getMyLabel()));

        for (Map.Entry<String, Variable> e : argToX.entrySet()) {
            String str = e.getKey();
            Variable xVar = e.getValue();
            Variable dest = convertToZ.get(xVar);
            String t = str.trim();

            Integer constant = tryParseInt(t);
            if (constant != null) {
                newInstructions.add(new ConstantAssignmentInstruction(dest, constant));
                continue;
            }

            Variable src = parseVariableToken(t);
            if (src != null) {
                newInstructions.add(new AssignmentInstruction(dest, src));
                continue;
            }

            String s=t;
            if (s.startsWith("(") && s.endsWith(")")) {
                s = s.substring(1, s.length() - 1).trim();
            }

            int depth = 0, cut = -1;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == ',' && depth == 0) { cut = i; break; }
            }

            String funcName = (cut == -1) ? s.trim() : s.substring(0, cut).trim();
            String funcArgs = (cut == -1) ? ""      : s.substring(cut + 1).trim();
            String funcUserString = program.getFunction(funcName).getUserString();

            newInstructions.add(new QuotationInstruction(dest, funcName, funcUserString, funcArgs));

        }

        convertToZ.put(y, prog.newWorkVar());

        for (Instruction ins : function.getInstructions()) {
           Label myLabel=FixedLabel.EMPTY;
            if (ins.getMyLabel() != FixedLabel.EMPTY) myLabel = newLabelMap.get(ins.getMyLabel());
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
                    Label target = newLabelMap.get(jnz.getTargetLabel());
                    newInstructions.add(new JumpNotZeroInstruction(v, target, myLabel));
                    break;
                }

                case JUMP_ZERO: {
                    JumpZeroInstruction jz = (JumpZeroInstruction) ins;
                    Variable v = convertToZ.get(jz.getVariable());
                    Label target = newLabelMap.get(jz.getTargetLabel());
                    newInstructions.add(new JumpZeroInstruction(v, target, myLabel));
                    break;
                }

                case JUMP_EQUAL_CONSTANT: {
                    JumpEqualConstantInstruction jec = (JumpEqualConstantInstruction) ins;
                    Variable v = convertToZ.get(jec.getVariable());
                    Label target = newLabelMap.get(jec.getTargetLabel());
                    int k = jec.getConstant(); // או getNumber() אם כך אצלך
                    newInstructions.add(new JumpEqualConstantInstruction(v, target, k, myLabel));
                    break;
                }

                case JUMP_EQUAL_VARIABLE: {
                    JumpEqualVariableInstruction jev = (JumpEqualVariableInstruction) ins;
                    Variable v1 = convertToZ.get(jev.getVariable());
                    Variable v2 = convertToZ.get(jev.getToCompare());
                    Label target = newLabelMap.get(jev.getTargetLabel());
                    newInstructions.add(new JumpEqualVariableInstruction(v1, target, v2, myLabel));
                    break;
                }

                case GOTO_LABEL: {
                    GoToInstruction go = (GoToInstruction) ins;
                    Variable v = convertToZ.get(go.getVariable());
                    Label target = newLabelMap.get(go.getTarget());
                    newInstructions.add(new GoToInstruction(v, target, myLabel));
                    break;
                }

                case JUMP_EQUAL_FUNCTION: {
                    JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) ins;
                    Variable v = convertToZ.get(jef.getVariable());
                    Label target = newLabelMap.get(jef.getTargetLabel());
                    String fname = jef.getFunctionName();
                    String ustr  = jef.getUserString();
                    String fargs = jef.getFunctionArguments();
                    newInstructions.add(new JumpEqualFunctionInstruction(v, target, fname, ustr, fargs, myLabel));
                    break;
                }

                case QUOTE: {
                    QuotationInstruction q = (QuotationInstruction) ins;
                    Variable dest = convertToZ.get(q.getVariable());
                    String fname = q.getFunctionName();
                    String ustr  = q.getUserString();
                    String fargs = q.getFunctionArguments();
                    newInstructions.add(new QuotationInstruction(dest, fname, ustr, fargs, myLabel));
                    break;
                }

                default:
            }
        }

        if(newLabelMap.containsKey(FixedLabel.EXIT) ) {
            newInstructions.add(new AssignmentInstruction(convertToZ.get(Variable.RESULT), getVariable(), newLabelMap.get(FixedLabel.EXIT)));
        }
        else newInstructions.add(new AssignmentInstruction(convertToZ.get(Variable.RESULT), getVariable()));

        return newInstructions;
    }

    private List<ArgVal> parseFunctionInputs(String argsString, ExecutionContext ctx) {
        List<ArgVal> out = new ArrayList<>();
        if (argsString == null) return out;

        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);
            if (c == '(') { depth++; cur.append(c); continue; }
            if (c == ')') { depth--; cur.append(c); continue; }
            if (c == ',' && depth == 0) { tokens.add(cur.toString().trim()); cur.setLength(0); continue; }
            cur.append(c);
        }
        tokens.add(cur.toString().trim());

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

    private Long evalArgVal(ArgVal a, ExecutionContext ctx, Program program) {
        if (a.getKind() == ArgKind.LONG) {
            return a.getLongValue();
        }
        String t = a.getText();

        if (t.startsWith("(") && t.endsWith(")")) {
            String inner = t.substring(1, t.length() - 1).trim();
            int depth = 0, cut = -1;
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == ',' && depth == 0) { cut = i; break; }
            }
            String fname = (cut == -1) ? inner.trim() : inner.substring(0, cut).trim();
            String fargs = (cut == -1) ? "" : inner.substring(cut + 1).trim();

            try {
                Function f = program.getFunction(fname);
                List<ArgVal> nested = parseFunctionInputs(fargs, ctx);

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

        int idx = 1;
        StringBuilder cur = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < functionArguments.length(); i++) {
            char c = functionArguments.charAt(i);
            if (c == '(') { depth++; cur.append(c); continue; }
            if (c == ')') { depth--; cur.append(c); continue; }
            if (c == ',' && depth == 0) {
                String key = cur.toString().trim();
                if (!key.isEmpty()) map.putIfAbsent(key, new VariableImpl(VariableType.INPUT, idx));
                idx++;
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }

        String last = cur.toString().trim();
        if (!last.isEmpty()) map.putIfAbsent(last, new VariableImpl(VariableType.INPUT, idx));

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
