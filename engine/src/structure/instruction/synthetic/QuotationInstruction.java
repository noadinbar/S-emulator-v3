package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.Label;
import structure.variable.Variable;

public class QuotationInstruction extends AbstractInstruction {

    private final String functionName;
    private final String userString;
    private final String functionArguments;

    public QuotationInstruction(Variable dest, String functionName,String userString ,String functionArguments) {
        super(InstructionKind.SYNTHETIC,InstructionType.QUOTE, dest, -1 );
        this.functionName=functionName;
        this.userString=userString;
        this.functionArguments=functionArguments;
    }

    public QuotationInstruction(Variable dest, String functionName, String userString, String functionArguments, Label myLabel) {
        super(InstructionKind.SYNTHETIC,InstructionType.QUOTE, dest, myLabel, -1 );
        this.functionName=functionName;
        this.userString=userString;
        this.functionArguments=functionArguments;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return null;
    }

    @Override
    public int cycles() { return 1; } // TODO: לעדכן לפי המדיניות שלך

    public String getFunctionName() { return functionName; }

    public String getFunctionArguments() { return functionArguments; }

    public String getUserString() { return userString; }
}
