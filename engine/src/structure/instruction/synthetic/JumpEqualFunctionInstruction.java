package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.Label;
import structure.variable.Variable;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final Label targetLabel;
    private final String functionName;
    private final String userString;
    private final String functionArguments;

    public JumpEqualFunctionInstruction(Variable variable, Label targetLabel, String functionName,String userString, String functionArguments) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_FUNCTION, variable, -1);
        this.targetLabel = targetLabel;
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    public JumpEqualFunctionInstruction(Variable variable, Label targetLabel, String functionName,String userString, String functionArguments, Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_FUNCTION, variable, myLabel, -1);
        this.targetLabel = targetLabel;
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    @Override
    public Label execute(ExecutionContext context) {
        // TODO: לוגיקת השוואה מול תוצאת פונקציה; כרגע שלד בלבד
        return null;
    }

    @Override
    public int cycles() {
        return 1; // TODO: לעדכן לפי המדיניות/טבלאות המחזורים שלך
    }

    public Label getTargetLabel() { return targetLabel; }
    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public String getUserString() { return userString; }
}
