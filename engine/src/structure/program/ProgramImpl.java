package structure.program;

import exceptions.UndefinedFunctionException;
import exceptions.UndefinedLabelException;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.function.Function;
import structure.instruction.Instruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.synthetic.*;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;
import structure.variable.VariableType;
import utils.InstructionsHelpers;
import utils.RunHistory;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramImpl implements Program, Serializable {

    private final String name;
    private final List<Instruction> instructions;
    private final List<RunHistory> runHistory = new ArrayList<>();
    private final List<Function> functions;
    private final Map<String, Function> stringFunctionMap = new LinkedHashMap<>();
    private int currentRunDegree = 0;
    private static final Pattern LBL_PATTERN = Pattern.compile("^L(\\d+)$");
    private final InstructionsHelpers helper = new InstructionsHelpers();
    private static final long serialVersionUID = 1L;

    public ProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
        functions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public List<Function> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    @Override
    public Function getFunction(String name) {
        Function func = stringFunctionMap.get(name);
        if (func == null) {
            throw new UndefinedFunctionException("Function '" + name + "' not found");
        }
        return func;
    }

    @Override
    public void addFunction(Function function) {
        if (function == null) return;
        String name = function.getName();
        functions.add(function);
        stringFunctionMap.put(name, function);
    }

    @Override
    public void validate() {
        Set<String> definedLabels = new HashSet<>();
        List<String> errors = new ArrayList<>();

        for (Instruction instr : instructions) {
            Label label = instr.getMyLabel();
            if (label != null && label != FixedLabel.EMPTY) {
                String rep = label.getLabelRepresentation();
                if (rep != null && !rep.isBlank()) {
                    definedLabels.add(rep);
                }
            }
        }


        for (int i = 0; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);

            Label targetLabel = switch (instr.getName()) {
                case "JUMP_NOT_ZERO" -> ((JumpNotZeroInstruction) instr).getTargetLabel();
                case "JUMP_ZERO" -> ((JumpZeroInstruction) instr).getTargetLabel();
                case "JUMP_EQUAL_CONSTANT" -> ((JumpEqualConstantInstruction) instr).getTargetLabel();
                case "JUMP_EQUAL_VARIABLE" -> ((JumpEqualVariableInstruction) instr).getTargetLabel();
                case "GOTO_LABEL" -> ((GoToInstruction) instr).getTarget();
                case "JUMP_EQUAL_FUNCTION" -> ((JumpEqualFunctionInstruction) instr).getTargetLabel();
                default -> null;
            };

            if (targetLabel != null &&
                    targetLabel != FixedLabel.EMPTY &&
                    !"EXIT".equals(targetLabel.getLabelRepresentation()) &&
                    !definedLabels.contains(targetLabel.getLabelRepresentation())) {
                errors.add("Undefined label '" + targetLabel.getLabelRepresentation() +
                        "' referenced at " + where(instr, i + 1) + " (main).");
            }
        }

        if (!errors.isEmpty()) {
            throw new UndefinedLabelException(
                    "Validation failed:\n" + String.join("\n", errors)
            );
        }
    }

    private void collectFunctionsInArgsOrdered(String args, List<String> out) {
        if (args == null || args.isBlank()) return;
        List<String> items = helper.splitTopArguments(args);
        for (String raw : items) {
            if (raw == null) continue;
            String t = raw.trim();
            if (t.isEmpty()) continue;

            if (t.charAt(0) == '(' && t.charAt(t.length() - 1) == ')') {
                String[] fa = helper.splitFuncNameAndArgs(t);
                String fname = (fa[0] == null) ? "" : fa[0].trim();
                if (!fname.isEmpty()) out.add(fname);
                collectFunctionsInArgsOrdered(fa[1], out);
            }
        }
    }

    private List<String> referencedFunctionsInOrder(Instruction instr) {
        List<String> out = new ArrayList<>();
        switch (instr.getName()) {
            case "QUOTE": {
                QuotationInstruction q = (QuotationInstruction) instr;
                String direct = q.getFunctionName();
                if (direct != null && !direct.trim().isEmpty()) out.add(direct.trim());
                collectFunctionsInArgsOrdered(q.getFunctionArguments(), out);
                break;
            }
            case "JUMP_EQUAL_FUNCTION": {
                JumpEqualFunctionInstruction jef = (JumpEqualFunctionInstruction) instr;
                String direct = jef.getFunctionName();
                if (direct != null && !direct.trim().isEmpty()) out.add(direct.trim());
                collectFunctionsInArgsOrdered(jef.getFunctionArguments(), out);
                break;
            }
            default: break;
        }
        return out;
    }

    private String where(Instruction instr, int oneBasedIndex) {
        Label lbl = instr.getMyLabel();
        if (lbl != null && lbl != FixedLabel.EMPTY) {
            String rep = lbl.getLabelRepresentation();
            if (rep != null && !rep.isBlank()) return "label " + rep;
        }
        return "instruction #" + oneBasedIndex;
    }

    public void setCurrentRunDegree(int degree) { this.currentRunDegree = Math.max(0, degree); }

    public void addRunHistory(List<Long> inputs, long yValue, int cycles) {
        int runNumber = runHistory.size() + 1;
        runHistory.add(new RunHistory(runNumber, currentRunDegree, inputs, yValue, cycles));
    }

    public List<RunHistory> getRunHistory() {
        return Collections.unmodifiableList(runHistory);
    }

    public int findMaxLabelIndex() {
        int max = 0;
        for (Instruction ins : this.getInstructions()) {
            Label lab = ins.getMyLabel();
            if (lab == null) continue;
            String name = lab.getLabelRepresentation();
            if (name == null) continue;
            Matcher m = LBL_PATTERN.matcher(name);
            if (m.matches()) {
                int idx = Integer.parseInt(m.group(1));
                if (idx > max) max = idx;
            }
        }
        return max;
    }

    public int findMaxWorkIndex() {
        int max = 0;

        for (Instruction ins : this.getInstructions()) {
            max = Math.max(max, workIndex(ins.getVariable()));

            switch (ins.getName()) {
                case "ASSIGNMENT": {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    max = Math.max(max, workIndex(a.getToAssign()));
                    break;
                }
                case "JUMP_EQUAL_VARIABLE": {
                    JumpEqualVariableInstruction j = (JumpEqualVariableInstruction) ins;
                    max = Math.max(max, workIndex(j.getToCompare()));
                    break;
                }
                default:
                    break;
            }
        }

        return max;
    }

    private static int workIndex(Variable v) {
        if (v == null || v.getType() != VariableType.WORK) return 0;
        String rep = v.getRepresentation();
        if (rep == null || rep.length() < 2) return 0;
        try {
            return Integer.parseInt(rep.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int calculateMaxDegree() {
        int degree = 0;
        while (true) {
            ExpandResult res = ProgramExpander.expandTo(this, degree);
            List<List<Instruction>> levels = res.getLevels();
            List<Instruction> lastLevel = levels.isEmpty()
                    ? List.of()
                    : levels.getLast();

            boolean hasSynthetic = false;
            for (Instruction ins : lastLevel) {
                if (Character.toUpperCase(ins.kind()) == 'S') {
                    hasSynthetic = true;
                    break;
                }
            }
            if (!hasSynthetic) {
                return degree;
            }
            degree++;
        }
    }
}
