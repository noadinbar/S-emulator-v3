package structure.instruction;

public enum InstructionKind {
    BASIC("B"),
    SYNTHETIC("S");

    private final char kind;

    InstructionKind(String kind) { this.kind=Character.toUpperCase(kind.trim().charAt(0)); }
    public char getKind() { return kind; }
    public boolean isBasic() { return this == BASIC; }
    public boolean isSynthetic() { return this == SYNTHETIC; }
}