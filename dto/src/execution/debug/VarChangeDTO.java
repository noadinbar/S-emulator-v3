package execution.debug;

import types.VarRefDTO;

public final class VarChangeDTO {
    private final VarRefDTO var;
    private final long before;
    private final long after;

    public VarChangeDTO(VarRefDTO var, long before, long after) {
        this.var = var;
        this.before = before;
        this.after = after;
    }

    public VarRefDTO getVar() { return var; }
    public long getBefore() { return before; }
    public long getAfter() { return after; }
}
