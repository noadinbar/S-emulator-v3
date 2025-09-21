package structure.instruction.synthetic;

import structure.execution.ExecutionContext;
import structure.execution.FunctionExecutor;
import structure.execution.FunctionExecutorImpl;
import structure.function.Function;
import structure.instruction.AbstractInstruction;
import structure.instruction.InstructionKind;
import structure.instruction.InstructionType;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest, -1);
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    public QuotationInstruction(Variable dest,
                                String functionName,
                                String userString,
                                String functionArguments,
                                Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest, myLabel, -1);
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    @Override
    public int cycles() {
        return InstructionType.QUOTE.getCycles() + Math.max(0, lastFunctionCycles);
    }

    public String getFunctionName() { return functionName; }
    public String getFunctionArguments() { return functionArguments; }
    public String getUserString() { return userString; }

    public Label execute(ExecutionContext context, Program program) {
        if (program == null) {
            context.updateVariable(getVariable(), 0L);
            lastFunctionCycles = 0;
            return FixedLabel.EMPTY;
        }

        Function function;
        try {
            function = program.getFunction(functionName);
        } catch (IllegalArgumentException e) {
            context.updateVariable(getVariable(), 0L);
            lastFunctionCycles = 0;
            return FixedLabel.EMPTY;
        }

        List<Long> inputsList = parseFunctionInputs(functionArguments, context);
        Long[] inputs = inputsList.toArray(new Long[0]);

        FunctionExecutor functionRunner = new FunctionExecutorImpl();
        long result = functionRunner.run(function,program, inputs);
        context.updateVariable(getVariable(), result);
        lastFunctionCycles = functionRunner.getLastRunCycles();

        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }

    private static List<Long> parseFunctionInputs(String argsString, ExecutionContext ctx) {
        List<Long> csvInputs = new ArrayList<>();
        if (argsString == null || argsString.isBlank()) return csvInputs;

        String[] parts = argsString.split(",");
        for (String raw : parts) {
            String variable = raw.trim();
            if (variable.isEmpty()) { csvInputs.add(0L); continue; }

            // מספר קבוע?
            try {
                csvInputs.add(Long.parseLong(variable));
                continue;
            } catch (NumberFormatException ignore) {}
            if ("y".equals(variable)) {
                csvInputs.add(ctx.getVariableValue(Variable.RESULT));
            } else if (variable.startsWith("x")) {
                csvInputs.add(ctx.getVariableValue(
                        new VariableImpl(VariableType.INPUT, parseInt(variable.substring(1)))
                ));
            } else if (variable.startsWith("z")) {
                csvInputs.add(ctx.getVariableValue(
                        new VariableImpl(VariableType.WORK, parseInt(variable.substring(1)))
                ));
            } else {
                csvInputs.add(0L);
            }
        }
        return csvInputs;
    }
}
