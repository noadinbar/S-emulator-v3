package structure.instruction.synthetic;

public final class ArgVal {
    private final ArgKind kind;
    private final Long longValue;
    private final String text;

    private ArgVal(ArgKind kind, Long longValue, String text) {
        this.kind = kind; this.longValue = longValue; this.text = text;
    }
    public static ArgVal ofLong(long v) { return new ArgVal(ArgKind.LONG, v, null); }
    public static ArgVal ofString(String s) { return new ArgVal(ArgKind.STRING, null, s); }

    public ArgKind getKind() { return kind; }
    public Long getLongValue() { return longValue; }
    public String getText() { return text; }
}
