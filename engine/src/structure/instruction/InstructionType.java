package structure.instruction;

public enum InstructionType {

    INCREASE("INCREASE", 1, "I", 5),
    DECREASE("DECREASE", 1, "I", 5),
    NEUTRAL("NEUTRAL", 0, "I", 5),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, "I", 5),
    ZERO_VARIABLE("ZERO_VARIABLE", 1, "II", 100),
    GOTO_LABEL("GOTO_LABEL", 1, "II", 100),
    ASSIGNMENT("ASSIGNMENT", 4, "III", 500),
    CONSTANT_ASSIGNMENT("CONSTANT_ASSIGNMENT", 2, "II", 100),
    JUMP_EQUAL_CONSTANT("JUMP_EQUAL_CONSTANT", 2, "III", 500),
    JUMP_EQUAL_VARIABLE("JUMP_EQUAL_VARIABLE", 2, "III", 500),
    JUMP_ZERO("JUMP_ZERO", 2, "III", 500),
    QUOTE("QUOTE", 5, "IV", 1000),
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION", 6, "IV", 1000)
    ;

    private final String name;
    private final int cycles;
    private final String generation;
    private final int credits;

    InstructionType(String name, int cycles, String generation, int credits) {
        this.name = name;
        this.cycles = cycles;
        this.generation = generation;
        this.credits = credits;
    }

    public String getName() {
        return name;
    }
    public int getCycles() {
        return cycles;
    }
    public String getGeneration() { return generation; }
    public int getCredits() { return credits; }
}
