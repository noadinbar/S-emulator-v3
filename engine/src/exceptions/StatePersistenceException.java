package exceptions;

public class StatePersistenceException extends RuntimeException {
    public StatePersistenceException(String message, Throwable cause) { super(message, cause); }
    public StatePersistenceException(String message) { super(message); }
}
