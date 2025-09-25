package exceptions;

public class UndefinedFunctionException extends RuntimeException {
    public UndefinedFunctionException(String message) {
        super(message);
    }
}
