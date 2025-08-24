package display;

import types.LabelDTO;
import types.VarRefDTO;

public class InstructionBodyDTO {
    private final InstrOpDTO op;

    private final VarRefDTO variable;     // למקרי INCREASE/DECREASE ול-JUMP על משתנה יחיד
    private final VarRefDTO dest;         // יעד השמה (ASSIGNMENT / CONSTANT_ASSIGNMENT / ZERO_VARIABLE)
    private final VarRefDTO source;       // מקור השמה (ASSIGNMENT)
    private final VarRefDTO compare;      // אופרנד שמאלי להשוואה (JUMP_EQUAL_VARIABLE)
    private final VarRefDTO compareWith;  // אופרנד ימני להשוואה (JUMP_EQUAL_VARIABLE)

    private final long constant;          // לשימוש ב-CONSTANT_ASSIGNMENT / JUMP_EQUAL_CONSTANT (0 אם לא בשימוש)
    private final LabelDTO jumpTo;        // יעד קפיצה / GoTo

    public InstructionBodyDTO(InstrOpDTO op,
                              VarRefDTO variable,
                              VarRefDTO dest,
                              VarRefDTO source,
                              VarRefDTO compare,
                              VarRefDTO compareWith,
                              long constant,
                              LabelDTO jumpTo) {
        this.op = op;
        this.variable = variable;
        this.dest = dest;
        this.source = source;
        this.compare = compare;
        this.compareWith = compareWith;
        this.constant = constant;
        this.jumpTo = jumpTo;
    }

    public InstrOpDTO getOp() { return op; }
    public VarRefDTO getVariable() { return variable; }
    public VarRefDTO getDest() { return dest; }
    public VarRefDTO getSource() { return source; }
    public VarRefDTO getCompare() { return compare; }
    public VarRefDTO getCompareWith() { return compareWith; }
    public long getConstant() { return constant; }
    public LabelDTO getJumpTo() { return jumpTo; }
}
