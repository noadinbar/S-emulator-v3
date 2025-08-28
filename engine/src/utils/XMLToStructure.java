package utils;

import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.instruction.synthetic.*;
import structure.label.FixedLabel;
import structure.label.LabelImpl;
import structure.program.ProgramImpl;
import structure.program.SProgram;
import structure.instruction.*;
import structure.label.Label;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

public class XMLToStructure {

    public ProgramImpl toProgram(SProgram sProgram) {
        ProgramImpl program = new ProgramImpl(sProgram.getName());

        sProgram.getSInstructions()
                .getSInstruction()
                .forEach(sInstr -> program.addInstruction(toInstruction(sInstr)));

        return program;
    }

    private Instruction toInstruction(SInstruction sInstruction) {
        InstructionType type = InstructionType.valueOf(sInstruction.getName().toUpperCase());
        Label label = sInstruction.getSLabel() != null ? new LabelImpl(sInstruction.getSLabel()) : null;


        Variable variable = extractVariable(sInstruction, null);

        switch (type) {
            case INCREASE:
                return label != null
                        ? new IncreaseInstruction(variable, label)
                        : new IncreaseInstruction(variable);

            case DECREASE:
                return label != null
                        ? new DecreaseInstruction(variable, label)
                        : new DecreaseInstruction(variable);

            case NEUTRAL:
                return label != null
                        ? new NeutralInstruction(variable, label)
                        : new NeutralInstruction(variable);

            case JUMP_NOT_ZERO:
                String targetLabelValue = getArgumentValue(sInstruction, "JNZLabel");
                Label targetLabel = targetLabelValue != null ? new LabelImpl(targetLabelValue) : null;
                return label != null
                        ? new JumpNotZeroInstruction(variable, targetLabel, label)
                        : new JumpNotZeroInstruction(variable, targetLabel);
            case ZERO_VARIABLE:
                return label != null
                        ? new ZeroVariableInstruction(variable, label)
                        : new ZeroVariableInstruction(variable);
            case GOTO_LABEL:
                String goToLabelValue = getArgumentValue(sInstruction, "gotoLabel");
                Label goToLabel = goToLabelValue != null ? new LabelImpl(goToLabelValue) : null;
                return label != null
                        ? new GoToInstruction(variable, goToLabel, label)
                        : new GoToInstruction(variable, goToLabel);
            case ASSIGNMENT:
                Variable source = extractVariable(sInstruction, "assignedVariable");
                return (label != null)
                        ? new AssignmentInstruction(variable, source, label)
                        : new AssignmentInstruction(variable, source);
            case CONSTANT_ASSIGNMENT:
                int constant = Integer.parseInt(getArgumentValue(sInstruction, "constantValue"));
                return (label != null)
                        ? new ConstantAssignmentInstruction(variable, constant, label)
                        : new ConstantAssignmentInstruction(variable, constant);
            case JUMP_ZERO:
                String destinationLabelValue = getArgumentValue(sInstruction, "JZLabel");
                Label destinationLabel = destinationLabelValue != null ? new LabelImpl(destinationLabelValue) : null;
                return label != null
                        ? new JumpZeroInstruction(variable, destinationLabel, label)
                        : new JumpZeroInstruction(variable, destinationLabel);
            case JUMP_EQUAL_CONSTANT:
                int constantValue = Integer.parseInt(getArgumentValue(sInstruction, "constantValue"));
                String jumpLabelValue = getArgumentValue(sInstruction, "JEConstantLabel");
                Label jumpLabel = jumpLabelValue != null ? new LabelImpl(jumpLabelValue) : null;
                return label != null
                        ? new JumpEqualConstantInstruction(variable, jumpLabel, constantValue, label)
                        : new JumpEqualConstantInstruction(variable, jumpLabel, constantValue);

            case JUMP_EQUAL_VARIABLE:
                Variable toCompare = extractVariable(sInstruction, "variableName");
                String jevText = getArgumentValue(sInstruction, "JEVariableLabel");
                Label jevLabel = null;
                if (jevText != null && !jevText.isBlank()) {
                    String normalizedLabel = jevText.trim();
                    jevLabel = "EXIT".equalsIgnoreCase(normalizedLabel) ? FixedLabel.EXIT : new LabelImpl(normalizedLabel);
                }
                return (label != null)
                        ? new JumpEqualVariableInstruction(variable, jevLabel, toCompare, label)
                        : new JumpEqualVariableInstruction(variable, jevLabel, toCompare);
            default:
                throw new IllegalArgumentException("Unknown instruction type: " + type);
    }
}

private String getArgumentValue(SInstruction sInstruction, String argName) {
    if (sInstruction.getSInstructionArguments() == null) {
        return null;
    }
    return sInstruction.getSInstructionArguments()
            .getSInstructionArgument()
            .stream()
            .filter(arg -> argName.equals(arg.getName()))
            .map(SInstructionArgument::getValue)
            .findFirst()
            .orElse(null);
}

    private Variable extractVariable(SInstruction sInstruction, String sourceName) {
        String variableText;
        if (sourceName == null || sourceName.isBlank()) {
            variableText = sInstruction.getSVariable();
        } else {
            variableText = getArgumentValue(sInstruction, sourceName);
        }

        if (variableText == null) return null;
        variableText = variableText.trim();
        if (variableText.isEmpty()) return null;

        char prefixChar = Character.toLowerCase(variableText.charAt(0)); // x / y / z
        VariableType type;
        switch (prefixChar) {
            case 'x': type = VariableType.INPUT;  break;
            case 'y': type = VariableType.RESULT; break;
            case 'z': type = VariableType.WORK;   break;
            default:
                throw new IllegalArgumentException(
                        "Unknown variable prefix: '" + prefixChar + "' in '" + variableText + "'"
                );
        }

        int index = 0;
        if (variableText.length() > 1) {
            String digits = variableText.substring(1);
            try {
                index = Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid variable index in '" + variableText + "' (expected digits after '" + prefixChar + "')",
                        e
                );
            }
        }

        return new VariableImpl(type, index);
    }

}

