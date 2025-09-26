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
        final Function function;
        function = program.getFunction(functionName);
        final int[] nestedCycles = {0};

        List<ArgVal> args = helper.parseFunctionInputs(functionArguments, context);
        Long[] inputs = new Long[args.size()];
        for (int i = 0; i < args.size(); i++) {
            inputs[i] = evalArgVal(args.get(i), context, program);
        }

        FunctionExecutor runner = new FunctionExecutorImpl();
        long qResult = runner.run(function, program, inputs);
        nestedCycles[0] += runner.getLastRunCycles();
        lastFunctionCycles = nestedCycles[0] + runner.getLastRunCycles();
        long v = context.getVariableValue(getVariable());
        return (v == qResult) ? targetLabel : FixedLabel.EMPTY;
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

    private List<Long> parseFunctionInputs(String argsString, ExecutionContext ctx) {
        List<Long> csvInputs = new ArrayList<>();
        if (argsString == null || argsString.isBlank()) return csvInputs;

        String[] parts = argsString.split(",");
        for (String raw : parts) {
            String token = raw.trim();
            if (token.isEmpty()) { csvInputs.add(0L); continue; }

            // מספר קבוע?
            try {
                csvInputs.add(Long.parseLong(token));
                continue;
            } catch (NumberFormatException ignore) {}

            if ("y".equals(token)) {
                csvInputs.add(ctx.getVariableValue(Variable.RESULT));
            } else if (token.startsWith("x")) {
                csvInputs.add(ctx.getVariableValue(
                        new VariableImpl(VariableType.INPUT, Integer.parseInt(token.substring(1)))
                ));
            } else if (token.startsWith("z")) {
                csvInputs.add(ctx.getVariableValue(
                        new VariableImpl(VariableType.WORK, Integer.parseInt(token.substring(1)))
                ));
            } else {
                csvInputs.add(0L);
            }
        }
        return csvInputs;
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
