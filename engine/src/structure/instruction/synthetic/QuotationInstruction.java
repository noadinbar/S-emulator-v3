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
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

import java.util.*;

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
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest, 1);
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    public QuotationInstruction(Variable dest,
                                String functionName,
                                String userString,
                                String functionArguments,
                                Label myLabel) {
        super(InstructionKind.SYNTHETIC, InstructionType.QUOTE, dest, myLabel, 1);
        this.functionName = functionName;
        this.userString = userString;
        this.functionArguments = functionArguments;
    }

    @Override
    public int cycles() {
        return InstructionType.QUOTE.getCycles() + Math.max(0, lastFunctionCycles);
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getFunctionArguments() {
        return functionArguments;
    }

    public String getUserString() {
        return userString;
    }

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
        long result = functionRunner.run(function, program, inputs);
        context.updateVariable(getVariable(), result);
        lastFunctionCycles = functionRunner.getLastRunCycles();

        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;
    }

    public List<Instruction> expand(ExpansionManager prog, Program program) {
        List<Instruction> newInstructions = new ArrayList<>();
        List<Instruction> functionInstructions = program.getFunction(functionName).getInstructions();

        Map<Variable, Variable> newVarMap = new LinkedHashMap<>();
        Map<Label, Label>       newLabelMap = new LinkedHashMap<>();

        java.util.function.Function<Variable, Variable> remapVar = (Variable v) -> {
            if (v == null) return null;
            return newVarMap.computeIfAbsent(v, k -> prog.newWorkVar());
        };
        java.util.function.Function<Label, Label> remapLabel = (Label l) -> {
            if (l == null || l == FixedLabel.EMPTY) return FixedLabel.EMPTY;
            return newLabelMap.computeIfAbsent(l, k -> prog.newLabel());
        };

        for (Instruction ins : functionInstructions) {
            InstructionType type = InstructionType.valueOf(ins.getName().toUpperCase());

            switch (type) {
                case INCREASE: {
                    Variable nv = remapVar.apply(ins.getVariable());
                    Label nl   = remapLabel.apply(ins.getMyLabel());
                    if (nl == FixedLabel.EMPTY) newInstructions.add(new IncreaseInstruction(nv));
                    else                        newInstructions.add(new IncreaseInstruction(nv, nl));
                    break;
                }

                case DECREASE: {
                    Variable nv = remapVar.apply(ins.getVariable());
                    Label nl   = remapLabel.apply(ins.getMyLabel());
                    if (nl == FixedLabel.EMPTY) newInstructions.add(new DecreaseInstruction(nv));
                    else                        newInstructions.add(new DecreaseInstruction(nv, nl));
                    break;
                }

                case NEUTRAL: {
                    Variable nv = remapVar.apply(ins.getVariable());
                    Label nl   = remapLabel.apply(ins.getMyLabel());
                    if (nl == FixedLabel.EMPTY) newInstructions.add(new NeutralInstruction(nv));
                    else                        newInstructions.add(new NeutralInstruction(nv, nl));
                    break;
                }

                case ZERO_VARIABLE: {
                    Variable nv = remapVar.apply(ins.getVariable());
                    Label nl   = remapLabel.apply(ins.getMyLabel());
                    if (nl == FixedLabel.EMPTY) newInstructions.add(new ZeroVariableInstruction(nv));
                    else                        newInstructions.add(new ZeroVariableInstruction(nv, nl));
                    break;
                }

                case ASSIGNMENT: {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    Variable dst = remapVar.apply(a.getVariable());
                    Variable src = remapVar.apply(a.getToAssign());
                    Label nl     = remapLabel.apply(a.getMyLabel());
                    newInstructions.add(new AssignmentInstruction(dst, src, nl));
                    break;
                }

                case CONSTANT_ASSIGNMENT: {
                    ConstantAssignmentInstruction c = (ConstantAssignmentInstruction) ins;
                    Variable dst = remapVar.apply(c.getVariable());
                    Label nl     = remapLabel.apply(c.getMyLabel());
                    newInstructions.add(new ConstantAssignmentInstruction(dst, c.getConstant(), nl));
                    break;
                }

                case JUMP_NOT_ZERO: {
                    JumpNotZeroInstruction j = (JumpNotZeroInstruction) ins;
                    Variable nv = remapVar.apply(j.getVariable());
                    Label nl    = remapLabel.apply(j.getMyLabel());
                    Label tgt   = remapLabel.apply(j.getTargetLabel());
                    newInstructions.add(new JumpNotZeroInstruction(nv, tgt, nl));
                    break;
                }

                case JUMP_ZERO: {
                    JumpZeroInstruction j = (JumpZeroInstruction) ins;
                    Variable nv = remapVar.apply(j.getVariable());
                    Label nl    = remapLabel.apply(j.getMyLabel());
                    Label tgt   = remapLabel.apply(j.getTargetLabel());
                    newInstructions.add(new JumpZeroInstruction(nv, tgt, nl));
                    break;
                }

                case JUMP_EQUAL_CONSTANT: {
                    JumpEqualConstantInstruction j = (JumpEqualConstantInstruction) ins;
                    Variable nv = remapVar.apply(j.getVariable());
                    Label nl    = remapLabel.apply(j.getMyLabel());
                    Label tgt   = remapLabel.apply(j.getTargetLabel());
                    newInstructions.add(new JumpEqualConstantInstruction(nv, tgt, j.getConstant(), nl));
                    break;
                }

                case JUMP_EQUAL_VARIABLE: {
                    JumpEqualVariableInstruction j = (JumpEqualVariableInstruction) ins;
                    Variable nv   = remapVar.apply(j.getVariable());
                    Variable comp = remapVar.apply(j.getToCompare());
                    Label nl      = remapLabel.apply(j.getMyLabel());
                    Label tgt     = remapLabel.apply(j.getTargetLabel());
                    newInstructions.add(new JumpEqualVariableInstruction(nv, tgt, comp, nl));
                    break;
                }

                case GOTO_LABEL: {
                    GoToInstruction g = (GoToInstruction) ins;
                    Variable nv = remapVar.apply(g.getVariable());
                    Label nl    = remapLabel.apply(g.getMyLabel());
                    Label tgt   = remapLabel.apply(g.getTarget());
                    newInstructions.add(new GoToInstruction(nv, tgt, nl));
                    break;
                }

                case QUOTE: {
                    QuotationInstruction q = (QuotationInstruction) ins;
                    Variable nv = remapVar.apply(q.getVariable());
                    Label nl    = remapLabel.apply(q.getMyLabel());
                    newInstructions.add(new QuotationInstruction(nv, q.getFunctionName(), q.getUserString(), q.getFunctionArguments(), nl));
                    break;
                }
                //TODO: Jumpfunction
                default:
                    break;
            }
        }
        return newInstructions;
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
