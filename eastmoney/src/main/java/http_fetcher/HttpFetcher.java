package http_fetcher;

public interface HttpFetcher {

    public String fetchContent(String url, int timeout) throws HttpFetcherException;
}