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
import structure.label.FixedLabel;
import structure.label.Label;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;
import utils.InstructionsHelpers;

import java.util.ArrayList;
import java.util.List;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final Label targetLabel;
    private final String functionName;
    private final String userString;
    private final String functionArguments;
    private int lastFunctionCycles = 0;
    private static InstructionsHelpers helper = new InstructionsHelpers();

    public JumpEqualFunctionInstruction(Variable variable, Label targetLabel, String functionName,String userString, String functionArguments) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_FUNCTION, variable);
        this.targetLabel = targetLabel;
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    public JumpEqualFunctionInstruction(Variable variable, Label targetLabel, String functionName,String userString, String functionArguments, Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.JUMP_EQUAL_FUNCTION, variable, myLabel);
        this.targetLabel = targetLabel;
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    @Override
    public int cycles() {
        return InstructionType.JUMP_EQUAL_FUNCTION.getCycles() + lastFunctionCycles;
    }

    public Label getTargetLabel() { return targetLabel; }
    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public String getUserString() { return userString; }

    @Override
    public Label execute(ExecutionContext context) {
        lastFunctionCycles = 0;
        return FixedLabel.EMPTY;
    }

    public Label execute(ExecutionContext context, Program program) {
        lastFunctionCycles = 0;
        List<ArgVal> inputsList = helper.parseFunctionInputs(functionArguments, context);
        Long[] nestedInputs = new Long[inputsList.size()];
        for (int i = 0; i < inputsList.size(); i++) {
            nestedInputs[i] = evalArgVal(inputsList.get(i), context, program);
        }
        Function f = program.getFunction(functionName);
        FunctionExecutor runner = new FunctionExecutorImpl();
        long val = runner.run(f, program, nestedInputs);
        lastFunctionCycles += runner.getLastRunCycles();
        long cur = context.getVariableValue(getVariable());
        return (cur == val) ? targetLabel : FixedLabel.EMPTY;
    }

    public List<Instruction> expand(ExpansionManager prog, Program program) {
        List <Instruction> newInstructions = new ArrayList<>();
        final Function function;
        function = program.getFunction(functionName);
        Variable z= prog.newWorkVar();
        Label myLabel=getMyLabel();
        if (myLabel==FixedLabel.EMPTY)
        {
            newInstructions.add(new QuotationInstruction(z, function.getName(), function.getUserString(), functionArguments));
        }
        else {
            newInstructions.add(new QuotationInstruction(z, function.getName(), function.getUserString(), functionArguments, myLabel));
        }

        newInstructions.add(new JumpEqualVariableInstruction(getVariable(), targetLabel, z));

        return newInstructions;
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
}
