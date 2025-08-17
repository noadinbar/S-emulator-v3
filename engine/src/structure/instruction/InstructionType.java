package structure.instruction;

public enum InstructionType {

    INCREASE("INCREASE", 1),
    DECREASE("DECREASE", 1),
    NEUTRAL("NO_OP", 0),
    JUMP_NOT_ZERO("JNZ", 3),
    ZERO_VARIABLE("ZERO_VARIABLE", 1),
    GOTO_LABEL("GOTO_LABEL", 1),
    ASSIGNMENT("ASSIGNMENT", 4),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2),
    JUMP_ZERO("JUMP_ZERO", 2),

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
