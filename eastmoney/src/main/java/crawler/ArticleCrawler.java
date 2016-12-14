package crawler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import entity.Article;
import entity.Comment;
import http_fetcher.HttpFetcher;
import http_fetcher.HttpFetcherException;
import http_fetcher.SimpleHttpFetcher;
import parser.ArticleParser;
import parser.ArticleParserInterface;
import parser.ParserException;
import record.RecordException;
import record.UrlProvider;

public class ArticleCrawler {

	private boolean isRunning = false;
	private int sleepSecond;
	private int waitSecond;
	private int timeoutSecond;
	private ExecutorService executor;
	private UrlProvider provider;
	private HttpFetcher fetcher = new SimpleHttpFetcher();
	private String baseUrl = "http://guba.eastmoney.com";
	private ArticleParserInterface parser = new ArticleParser();

	public ArticleCrawler(UrlProvider provider) {
		this.provider = provider;
	}

	public void start(int threadAmount, int sleepSecond, int waitSecond, int timeoutSecond) {
		if (isRunning) {
			return;
		}

		System.out.println("Start article crawler, [threadAmount=" + threadAmount + ", sleepSecond=" + sleepSecond
				+ ", waitSecond=" + waitSecond + ", timeoutSecond=" + timeoutSecond + "].");
		this.sleepSecond = sleepSecond;
		this.waitSecond = waitSecond;
		executor = Executors.newFixedThreadPool(threadAmount);
		for (int i = 0; i < threadAmount; i++) {
			executor.execute(new CrawlerHelper());
		}

		isRunning = true;
	}

	public void shutdown() {
		if (!isRunning) {
			return;
		}
		System.out.println("Stop article crawler.");
		executor.shutdownNow();
		executor = null;
		isRunning = false;
	}

	private class CrawlerHelper extends Thread {

		@Override
		public void run() {

			System.out.println("Crawler thread[" + Thread.currentThread().getId() + "] starts.");

			// 每次循环都尝试爬取一篇文章（包括它的所有评论）
			loop: while (true) {
				String url = null;
				try {
					url = provider.nextUrl();
				} catch (RecordException e1) {
					e1.printStackTrace();
				}
				if (url != null) {
					Article article = null;
					try {

						// 组装url，爬页面
						url = baseUrl + url;
						System.out.println("Crawling page [" + url + "].");
						String content = fetcher.fetchContent(url, timeoutSecond * 1000);
						System.out.println("Crawled  page [" + url + "].");

						// 解析文章和第一页的评论
						article = parser.parseArticle(content);
						System.out.println("Parsed article [" + url + "].");
						List<Comment> comments = parser.parseComments(content);
						System.out.println("Parsed comments [" + url + "].");
						article.addComments(comments);

						// 循环爬取所有评论
						int totalPages = article.getTotalPages();
						int dotPos = url.lastIndexOf('.'); // 原页面的url假设为http://xxx.html，那么评论第二页的格式则是http://xxx_2.html
						if (dotPos != -1) {
							String p1 = url.substring(0, dotPos);
							String p2 = url.substring(dotPos + 1);
							for (int i = 2; i <= totalPages; i++) {

								try {
									Thread.sleep(1000 * sleepSecond);
								} catch (InterruptedException e) {
									e.printStackTrace();
									break loop;
								}

								try {
									String subUrl = p1 + "_" + i + "." + p2;
									System.out.println("Crawling sub page [" + subUrl + "].");
									String subContent = fetcher.fetchContent(subUrl, timeoutSecond * 1000);
									System.out.println("Crawled  page [" + subUrl + "].");
									List<Comment> subComments = parser.parseComments(subContent);
									article.addComments(subComments);
									System.out.println("Parsed comments [" + subUrl + "].");
								} catch (Exception e) {
									// 爬评论的时候，发生任何错误都可以容忍，不会影响下一页评论的爬虫，也不会影响已经爬取的内容
									e.printStackTrace();
								}
							}
						}
						provider.addArticle(url, article);

					} catch (HttpFetcherException e) {
						e.printStackTrace();
					} catch (ParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						Thread.sleep(1000 * waitSecond);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break loop;
					}
				}
				try {
					Thread.sleep(1000 * sleepSecond);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break loop;
				}
			}
			System.out.println("Crawler thread[" + Thread.currentThread().getId() + "] quits.");
		}
	}
}
