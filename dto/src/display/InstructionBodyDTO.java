package display;

import types.LabelDTO;
import types.VarRefDTO;

public class InstructionBodyDTO {
    private final InstrOpDTO op;

    private final VarRefDTO variable;
    private final VarRefDTO dest;
    private final VarRefDTO source;
    private final VarRefDTO compare;
    private final VarRefDTO compareWith;
    private final long constant;
    private final LabelDTO jumpTo;
    private final String userString;
    private final String functionArgs;

    public InstructionBodyDTO(InstrOpDTO op,
                              VarRefDTO variable,
                              VarRefDTO dest,
                              VarRefDTO source,
                              VarRefDTO compare,
                              VarRefDTO compareWith,
                              long constant,
                              LabelDTO jumpTo) {
        this(op, variable, dest, source, compare, compareWith, constant, jumpTo, null, null, null);
    }

    public InstructionBodyDTO(InstrOpDTO op,
                              VarRefDTO variable,
                              VarRefDTO dest,
                              VarRefDTO source,
                              VarRefDTO compare,
                              VarRefDTO compareWith,
                              long constant,
                              LabelDTO jumpTo,
                              String functionName,
                              String userString,
                              String functionArgs) {
        this.op = op;
        this.variable = variable;
        this.dest = dest;
        this.source = source;
        this.compare = compare;
        this.compareWith = compareWith;
        this.constant = constant;
        this.jumpTo = jumpTo;
        this.userString = userString;
        this.functionArgs=functionArgs;
    }

    public InstrOpDTO getOp() { return op; }
    public VarRefDTO getVariable() { return variable; }
    public VarRefDTO getDest() { return dest; }
    public VarRefDTO getSource() { return source; }
    public VarRefDTO getCompare() { return compare; }
    public VarRefDTO getCompareWith() { return compareWith; }
    public long getConstant() { return constant; }
    public LabelDTO getJumpTo() { return jumpTo; }
    public String getUserString() { return userString; }
    public String getFunctionArgs() { return functionArgs; }
}
