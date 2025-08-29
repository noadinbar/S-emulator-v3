package structure.program;

import structure.instruction.Instruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.synthetic.*;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;
import structure.variable.VariableType;
import utils.ParseResult;
import utils.RunHistory;
import exceptions.UndefinedLabelException;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramImpl implements Program, Serializable {

    private final String name;
    private final List<Instruction> instructions;
    private final List<RunHistory> runHistory = new ArrayList<>();
    private int currentRunDegree = 0;
    private static final Pattern LBL_PATTERN = Pattern.compile("^L(\\d+)$");
    private static final long serialVersionUID = 1L;

    public ProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
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
    public ParseResult validate() {
        Set<String> definedLabels = new HashSet<>();
        for (Instruction instr : instructions) {
            Label label = instr.getMyLabel();
            if (label != null && label != FixedLabel.EMPTY) {
                definedLabels.add(label.getLabelRepresentation());
            }
        }

        for (Instruction instr : instructions) {
            Label targetLabel = null;

            switch (instr.getName()) {
                case "JUMP_NOT_ZERO":
                    targetLabel = ((JumpNotZeroInstruction) instr).getTargetLabel();
                    break;
                case "JUMP_ZERO":
                    targetLabel = ((JumpZeroInstruction) instr).getTargetLabel();
                    break;
                case "JUMP_EQUAL_CONSTANT":
                    targetLabel = ((JumpEqualConstantInstruction) instr).getTargetLabel();
                    break;
                case "JUMP_EQUAL_VARIABLE":
                    targetLabel = ((JumpEqualVariableInstruction) instr).getTargetLabel();
                    break;
                case "GOTO_LABEL":
                    targetLabel = ((GoToInstruction) instr).getTarget();
                    break;
                default:

                    break;
            }


            if (isExit(targetLabel)) continue;

            if (targetLabel != null &&
                    targetLabel != FixedLabel.EMPTY &&
                    !definedLabels.contains(targetLabel.getLabelRepresentation())) {

                throw new UndefinedLabelException(
                        "Label '" + targetLabel.getLabelRepresentation() + "' is referenced but not defined."
                );
            }
        }

        return ParseResult.success("Program validation passed successfully.");
    }

    public void setCurrentRunDegree(int degree) { this.currentRunDegree = Math.max(0, degree); }

    public int getCurrentRunDegree() { return currentRunDegree; }

    public void addRunHistory(List<Long> inputs, long yValue, int cycles) {
        int runNumber = runHistory.size() + 1;
        runHistory.add(new RunHistory(runNumber, currentRunDegree, inputs, yValue, cycles));
    }

    public List<RunHistory> getRunHistory() {
        return Collections.unmodifiableList(runHistory);
    }

    public void clearRunHistory() { runHistory.clear(); }

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
        return instructions.stream()
                .mapToInt(Instruction::getDegree)
                .max()
                .orElse(0);
    }

    @Override
    public int calculateCycles() {
        int cycles = 0;
        for (Instruction ins : getInstructions()) {
            cycles += ins.cycles();
        }
        return cycles;
    }

    private static boolean isExit(Label l) {
        if (l == null) return false;
        if (l == FixedLabel.EXIT) return true;
        String rep = l.getLabelRepresentation();
        return "EXIT".equals(rep);
    }



}
