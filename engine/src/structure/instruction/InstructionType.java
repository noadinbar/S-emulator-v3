package structure.instruction;

public enum InstructionType {

    INCREASE("INCREASE", 1),
    DECREASE("DECREASE", 1),
    NEUTRAL("NEUTRAL", 0),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2),
    ZERO_VARIABLE("ZERO_VARIABLE", 1),
    GOTO_LABEL("GOTO_LABEL", 1),
    ASSIGNMENT("ASSIGNMENT", 4),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2),
    JUMP_ZERO("JUMP_ZERO", 2),
    QUOTE("QUOTE", 5),
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION", 6)
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
