package http_fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A very simple solution for HTTP fetcher.
 */
final public class SimpleHttpFetcher implements HttpFetcher {

    @Override
    public String fetchContent(String url) throws HttpFetcherException {

        try {

            // Open the connection
            URL u = new URL(url);
            URLConnection connection = u.openConnection();
            connection.setReadTimeout(3000);
            HttpURLConnection htCon = (HttpURLConnection) connection;
            int res = htCon.getResponseCode();

            // Check the response code
            if (res != HttpURLConnection.HTTP_OK) {
                throw new HttpFetcherException("Response Code is " + res);
            }

            // Read the response data
            BufferedReader in = new BufferedReader(new InputStreamReader(htCon.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line).append("\n");
            }
            in.close();
            return content.toString();

        } catch (MalformedURLException e) {
            throw new HttpFetcherException("Malformed URL", e);
        } catch (IOException e) {
            throw new HttpFetcherException("Cannot open URL", e);
        }
    }

}
