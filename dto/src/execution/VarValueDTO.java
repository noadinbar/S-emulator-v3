package execution;

import types.VarRefDTO;

public class VarValueDTO {
    private final VarRefDTO var;
    private final long value;
    public VarValueDTO(VarRefDTO var, long value) { this.var = var; this.value = value; }
    public VarRefDTO getVar() { return var; }
    public long getValue() { return value; }
}