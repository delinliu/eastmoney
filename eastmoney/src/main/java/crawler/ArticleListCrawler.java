package crawler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import http_fetcher.HttpFetcher;
import http_fetcher.HttpFetcherException;
import http_fetcher.SimpleHttpFetcher;
import parser.ListParser;
import parser.ListParserInterface;
import parser.ParserException;
import record.UrlReceiver;

public class ArticleListCrawler {

	private Logger logger = LoggerFactory.getLogger(ArticleListCrawler.class);

	private boolean isRunning;
	private UrlReceiver receiver;
	private ExecutorService executor;
	private AtomicInteger page;
	private int maxPage = 700000;
	private int timeoutSecond;
	private int sleepSecond;
	private String urlTemplate = "http://guba.eastmoney.com/default_%d.html";
	private HttpFetcher fetcher = new SimpleHttpFetcher();
	private ListParserInterface parser = new ListParser();

	public ArticleListCrawler(UrlReceiver receiver) {
		this.receiver = receiver;
		page = new AtomicInteger(receiver.getPageAmount());
	}

	public void start(int threadAmount, int sleepSecond, int timeoutSecond) {
		if (isRunning) {
			return;
		}

		logger.info("List crawler starts, [threadAmount=" + threadAmount + ", sleepSecond=" + sleepSecond
				+ ", timeoutSecond=" + timeoutSecond + "].");
		this.timeoutSecond = timeoutSecond;
		this.sleepSecond = sleepSecond;
		executor = Executors.newFixedThreadPool(threadAmount);
		for (int i = 0; i < threadAmount; i++) {
			executor.execute(new CrawlerHelper());
		}

		isRunning = true;
	}

	public void stop() {
		if (!isRunning) {
			return;
		}
		logger.info("List crawler stops.");
		executor.shutdownNow();
		isRunning = false;
	}

	private class CrawlerHelper extends Thread {

		@Override
		public void run() {

			logger.info("Article list crawler[" + Thread.currentThread().getId() + "] start.");

			loop: while (true) {
				int currPage = page.getAndIncrement();
				if (currPage > maxPage) {
					logger.info("Article list crawler[" + Thread.currentThread().getId() + "] finished.");
					break loop;
				}
				String url = String.format(urlTemplate, currPage);
				logger.info("Crawling list page [" + url + "].");
				try {
					String content = fetcher.fetchContent(url, 1000 * timeoutSecond);
					logger.info("Crawled list page [" + url + "].");
					Set<String> urls = parser.parseList(content);
					logger.info("Parsed list page [" + url + "].");
					receiver.addOnePageList(urls);
				} catch (HttpFetcherException e) {
					logger.error(null, e);
				} catch (ParserException e) {
					logger.error(null, e);
				} catch (IOException e) {
					logger.error(null, e);
				}
				try {
					Thread.sleep(1000 * sleepSecond);
				} catch (InterruptedException e) {
					logger.error(null, e);
					break loop;
				}
			}

			logger.info("Article list crawler[" + Thread.currentThread().getId() + "] stop.");
		}
	}
}
