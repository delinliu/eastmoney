package http_fetcher;

public class HttpFetcherException extends Exception {

    private static final long serialVersionUID = 4212457083766437116L;

    public HttpFetcherException(String message) {
        super(message);
    }

    public HttpFetcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
