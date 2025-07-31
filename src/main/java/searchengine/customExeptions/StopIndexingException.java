package searchengine.customExeptions;

public class StopIndexingException extends RuntimeException {
    public StopIndexingException(String message) {
        super(message);
    }
}
