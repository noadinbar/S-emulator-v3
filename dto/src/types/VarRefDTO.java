package types;

public class VarRefDTO {
    private final VarOptionsDTO variable;
    private final int index;

    public VarRefDTO(VarOptionsDTO var, int index) {
        this.variable = var;
        this.index = index;
    }

    public VarOptionsDTO getVariable() { return variable; }
    public int getIndex() { return index; }
}