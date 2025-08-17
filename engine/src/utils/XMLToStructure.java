package utils;

import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NoOpInstruction;
import structure.instruction.synthetic.*;
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

            case NEUTRAL:
                return label != null
                        ? new NoOpInstruction(variable, label)
                        : new NoOpInstruction(variable);

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
                String srcVal = getArgumentValue(sInstruction, "assignedVariable");
                char sHead = Character.toLowerCase(srcVal.charAt(0)); // 'x'/'z'/'y'
                VariableType srcType = (sHead == 'x') ? VariableType.INPUT
                        : (sHead == 'z') ? VariableType.WORK
                        : VariableType.RESULT;
                int srcNum = (sHead == 'y') ? 0 : Integer.parseInt(srcVal.substring(1));
                Variable src = new VariableImpl(srcType, srcNum);

                return (label != null)
                        ? new AssignmentInstruction(variable, src, label)
                        : new AssignmentInstruction(variable, src);
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
                String toCompareValue = getArgumentValue(sInstruction, "variableName");
                Variable toCompare = new VariableImpl(VariableType.valueOf(toCompareValue.substring(0, 1)), Integer.parseInt(toCompareValue.substring(1)));
                String argumentValue = getArgumentValue(sInstruction, "JEVariableLabel");
                Label argumentLabel = argumentValue != null ? new LabelImpl(argumentValue) : null;
                return label != null
                        ? new JumpEqualVariableInstruction(variable, argumentLabel, toCompare, label)
                        : new JumpEqualVariableInstruction(variable, argumentLabel, toCompare);



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

