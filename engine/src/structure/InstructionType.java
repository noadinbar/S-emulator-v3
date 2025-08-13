package structure;

public enum InstructionType {
    BASIC, SYNTHETIC;

    public static InstructionType fromString(String value) {
        if (value == null) return null;
        switch (value.trim().toLowerCase()) {
            case "basic": return BASIC;
            case "synthetic": return SYNTHETIC;
            default: return null;
        }
    }
}
