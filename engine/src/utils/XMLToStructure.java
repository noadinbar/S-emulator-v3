package utils;

import structure.label.LabelImpl;
import structure.program.ProgramImpl;
import structure.program.SProgram;
import structure.instruction.*;
import structure.label.Label;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.List;
import java.util.stream.Collectors;

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

        // יצירת LabelImpl אם קיים ב-SInstruction
        Label label = sInstruction.getSLabel() != null ? new LabelImpl(sInstruction.getSLabel()) : null;

        // שליפת ה-variable argument והמרה ל-VariableImpl
        String variableValue = getArgumentValue(sInstruction, "variable");
        Variable variable = null;
        if (variableValue != null && variableValue.length() > 1) {
            VariableType varType = VariableType.valueOf(variableValue.substring(0, 1));
            int varNumber = Integer.parseInt(variableValue.substring(1));
            variable = new VariableImpl(varType, varNumber);
        }

        switch (type) {
            case INCREASE:
                return label != null
                        ? new IncreaseInstruction(variable, label)
                        : new IncreaseInstruction(variable);

            case DECREASE:
                return label != null
                        ? new DecreaseInstruction(variable, label)
                        : new DecreaseInstruction(variable);

            case NO_OP:
                return label != null
                        ? new NoOpInstruction(variable, label)
                        : new NoOpInstruction(variable);

            case JUMP_NOT_ZERO:
                String targetLabelValue = getArgumentValue(sInstruction, "targetLabel");
                Label targetLabel = targetLabelValue != null ? new LabelImpl(targetLabelValue) : null;
                return label != null
                        ? new JumpNotZeroInstruction(variable, targetLabel, label)
                        : new JumpNotZeroInstruction(variable, targetLabel);

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


}
