package http_fetcher;

import org.junit.Assert;
import org.junit.Test;

public class SimpleHttpFetcherTest {

	static final String normalUrls[] = { "http://sh.ziroom.com/z/nl/z2-s6%E5%8F%B7%E7%BA%BF.html",
			"http://sh.ziroom.com/" };
	static final String notFoundUrl = "http://sh.ziroom.com/notfound";
	static final String malformedUrl = "malformed url";
	static final String cannotOpenUrl = "http://something.cannot.open";
	static final int timeout = 3000;

	@Test
	public void testFetchContentSuccess() throws HttpFetcherException {
		SimpleHttpFetcher fetcher = new SimpleHttpFetcher();
		for (String url : normalUrls) {
			Assert.assertNotNull(fetcher.fetchContent(url, timeout));
		}
	}

	@Test
	public void testFetchContentExceptionMalformedURL() {
		SimpleHttpFetcher fetcher = new SimpleHttpFetcher();
		try {
			fetcher.fetchContent(malformedUrl, timeout);
			Assert.assertTrue(false);
		} catch (HttpFetcherException e) {
			Assert.assertEquals("Malformed URL", e.getMessage());
		}
	}

	@Test
	public void testFetchContentExceptionNotFoundURL() {
		SimpleHttpFetcher fetcher = new SimpleHttpFetcher();
		try {
			fetcher.fetchContent(notFoundUrl, timeout);
			Assert.assertTrue(false);
		} catch (HttpFetcherException e) {
			Assert.assertEquals("Response Code is 404", e.getMessage());
		}
	}

	@Test
	public void testFetchContentExceptionCannotOpenURL() {
		SimpleHttpFetcher fetcher = new SimpleHttpFetcher();
		try {
			fetcher.fetchContent(cannotOpenUrl, timeout);
			Assert.assertTrue(false);
		} catch (HttpFetcherException e) {
			Assert.assertEquals("Cannot open URL", e.getMessage());
		}
	}
}
