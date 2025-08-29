package exceptions;

public class ProgramNotLoadedException extends RuntimeException {
    public ProgramNotLoadedException(String message) {
        super(message);
    }
}