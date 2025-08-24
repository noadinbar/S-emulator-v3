package types;

public class VarRefDTO {
    private final VarSpace space;
    private final int index;

    public VarRefDTO(VarSpace space, int index) {
        this.space = space;
        this.index = index;
    }

    public VarSpace getSpace() { return space; }
    public int getIndex() { return index; }
}