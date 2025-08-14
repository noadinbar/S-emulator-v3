package structure.instruction;

public enum InstructionType {

    INCREASE("INCREASE", 1),
    DECREASE("DECREASE", 1),
    NO_OP("NO_OP", 0),
    JUMP_NOT_ZERO("JNZ", 3)

    ;

    private final String name;
    private final int cycles;

    InstructionType(String name, int cycles) {
        this.name = name;
        this.cycles = cycles;
    }

    public String getName() {
        return name;
    }

    public int getCycles() {
        return cycles;
    }
}
