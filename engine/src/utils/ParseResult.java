package utils;

public class ParseResult {
    private final boolean success;
    private final String message;

    private ParseResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ParseResult success(String message) {
        return new ParseResult(true, message);
    }

    public static ParseResult error(String message) {
        return new ParseResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
