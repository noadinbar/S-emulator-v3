package utils;
import structure.program.SProgram;

public final class ParseResult {

    private final boolean success;
    private final String message;
    private final SProgram program;

    // בנאי פרטי כדי לשלוט על יצירת האובייקט
    private ParseResult(boolean success, String message, SProgram program) {
        this.success = success;
        this.message = message;
        this.program = program;
    }

    // יצירת תוצאה מוצלחת
    public static ParseResult success(SProgram program) {
        return new ParseResult(true,
                "Program '" + program.getName() + "' loaded successfully.",
                program);
    }

    // יצירת תוצאה שגויה
    public static ParseResult error(String message) {
        return new ParseResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public SProgram getProgram() {
        return program;
    }
}
