package crawler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import http_fetcher.HttpFetcher;
import http_fetcher.HttpFetcherException;
import http_fetcher.SimpleHttpFetcher;
import parser.ListParser;
import parser.ListParserInterface;
import parser.ParserException;
import record.UrlReceiver;

public class ArticleListCrawler {

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

		System.out.println("List crawler starts.");
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
		System.out.println("List crawler stops.");
		executor.shutdownNow();
		isRunning = false;
	}

	private class CrawlerHelper extends Thread {

		@Override
		public void run() {

			System.out.println("Article list crawler[" + Thread.currentThread().getId() + "] start.");

			loop: while (true) {
				int currPage = page.getAndIncrement();
				if (currPage > maxPage) {
					System.out.println("Article list crawler[" + Thread.currentThread().getId() + "] finished.");
					break loop;
				}
				String url = String.format(urlTemplate, currPage);
				System.out.println("Crawling list page [" + url + "].");
				try {
					String content = fetcher.fetchContent(url, 1000 * timeoutSecond);
					System.out.println("Crawled list page [" + url + "].");
					Set<String> urls = parser.parseList(content);
					System.out.println("Parsed list page [" + url + "].");
					receiver.addOnePageList(urls);
				} catch (HttpFetcherException e) {
					e.printStackTrace();
				} catch (ParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(1000 * sleepSecond);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break loop;
				}
			}

			System.out.println("Article list crawler[" + Thread.currentThread().getId() + "] stop.");
		}
	}
}
