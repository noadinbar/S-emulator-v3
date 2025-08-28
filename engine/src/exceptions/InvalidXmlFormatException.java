package exceptions;

public class InvalidXmlFormatException extends RuntimeException {
    public InvalidXmlFormatException(String message) { super(message); }
    public InvalidXmlFormatException(String message, Throwable cause) { super(message, cause); }
}
