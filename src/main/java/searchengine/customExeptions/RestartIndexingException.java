package searchengine.customExeptions;

public class RestartIndexingException extends RuntimeException {
    public RestartIndexingException(String message) {
        super(message);
    }
}
