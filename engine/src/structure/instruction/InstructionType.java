package structure.instruction;

public enum InstructionType {

    INCREASE("INCREASE", 1, "I"),
    DECREASE("DECREASE", 1, "I"),
    NEUTRAL("NEUTRAL", 0, "I"),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, "I"),
    ZERO_VARIABLE("ZERO_VARIABLE", 1, "II"),
    GOTO_LABEL("GOTO_LABEL", 1, "II"),
    ASSIGNMENT("ASSIGNMENT", 4, "III"),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, "II"),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, "III"),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, "III"),
    JUMP_ZERO("JUMP_ZERO", 2, "III"),
    QUOTE("QUOTE", 5, "IV"),
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION", 6, "IV")
    ;

    private final String name;
    private final int cycles;
    private final String generation;

    InstructionType(String name, int cycles, String generation) {
        this.name = name;
        this.cycles = cycles;
        this.generation = generation;
    }

    public String getName() {
        return name;
    }
    public int getCycles() {
        return cycles;
    }
    public String getGeneration() { return generation; }
}
