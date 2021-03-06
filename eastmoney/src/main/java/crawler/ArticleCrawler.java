package crawler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private Logger logger = LoggerFactory.getLogger(ArticleCrawler.class);

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

		logger.info("Start article crawler, [threadAmount=" + threadAmount + ", sleepSecond=" + sleepSecond
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
		logger.info("Stop article crawler.");
		executor.shutdownNow();
		executor = null;
		isRunning = false;
	}

	private class CrawlerHelper extends Thread {

		@Override
		public void run() {

			long threadId = Thread.currentThread().getId();

			logger.info("Crawler thread[" + threadId + "] starts.");

			// 每次循环都尝试爬取一篇文章（包括它的所有评论）
			loop: while (true) {
				long loopBegin = System.currentTimeMillis();
				long begin, end;
				String url = null;
				try {
					begin = System.currentTimeMillis();
					url = provider.nextUrl();
					end = System.currentTimeMillis();
					logger.info("[" + threadId + "] Get url time [" + (end - begin) + "].");
				} catch (RecordException e1) {
					e1.printStackTrace();
				}
				if (url != null) {
					Article article = null;

					// 组装url
					String partUrl = url;
					url = baseUrl + partUrl;

					try {

						// 爬页面
						logger.info("[" + threadId + "] Crawling page [" + url + "].");
						begin = System.currentTimeMillis();
						String content = fetcher.fetchContent(url, timeoutSecond * 1000);
						end = System.currentTimeMillis();
						logger.info("[" + threadId + "] Crawled  page [" + url + "].");
						logger.info("[" + threadId + "] Crawling time [" + (end - begin) + "].");

						// 解析文章和第一页的评论
						begin = System.currentTimeMillis();
						article = parser.parseArticle(content);
						logger.info("[" + threadId + "] Parsed article [" + url + "].");
						List<Comment> comments = parser.parseComments(content);
						logger.info("[" + threadId + "] Parsed comments [" + url + "].");
						article.addComments(comments);
						article.setFirstPageUrl(url);
						end = System.currentTimeMillis();
						logger.info("[" + threadId + "] Parse time [" + (end - begin) + "].");

						// 循环爬取所有评论
						begin = System.currentTimeMillis();
						int totalPages = article.getTotalPages();
						int dotPos = url.lastIndexOf('.'); // 原页面的url假设为http://xxx.html，那么评论第二页的格式则是http://xxx_2.html
						if (dotPos != -1) {
							String p1 = url.substring(0, dotPos);
							String p2 = url.substring(dotPos + 1);
							for (int i = 2; i <= totalPages; i++) {

								try {
									Thread.sleep(1000 * sleepSecond);
								} catch (InterruptedException e) {
									logger.error(null, e);
									break loop;
								}

								try {
									String subUrl = p1 + "_" + i + "." + p2;
									logger.info("[" + threadId + "] Crawling sub page [" + subUrl + "].");
									String subContent = fetcher.fetchContent(subUrl, timeoutSecond * 1000);
									logger.info("[" + threadId + "] Crawled  page [" + subUrl + "].");
									List<Comment> subComments = parser.parseComments(subContent);
									article.addComments(subComments);
									logger.info("[" + threadId + "] Parsed comments [" + subUrl + "].");
								} catch (Exception e) {
									// 爬评论的时候，发生任何错误都可以容忍，不会影响下一页评论的爬虫，也不会影响已经爬取的内容
									logger.error(null, e);
								}
							}
						}
						end = System.currentTimeMillis();
						logger.info("[" + threadId + "] Crawling comments time [" + (end - begin) + "].");

						begin = System.currentTimeMillis();
						provider.addArticle(partUrl, article);
						end = System.currentTimeMillis();
						logger.info("Add article time [" + (end - begin) + "].");

					} catch (HttpFetcherException e) {
						logger.error(null, e);
					} catch (ParserException e) {
						if ("Article removed.".equals(e.getMessage())) {
							logger.info("[" + threadId + "] Article removed [" + url + "].");
							try {
								provider.addRemovedUrl(partUrl);
							} catch (Exception ee) {
								logger.error(null, ee);
							}
						} else {
							logger.error(null, e);
						}
					} catch (IOException e) {
						logger.error(null, e);
					} catch (Exception e) {
						logger.error(null, e);
					}
				} else {
					try {
						Thread.sleep(1000 * waitSecond);
					} catch (InterruptedException e) {
						logger.error(null, e);
						break loop;
					}
				}
				begin = System.currentTimeMillis();
				try {
					Thread.sleep(1000 * sleepSecond);
				} catch (InterruptedException e) {
					logger.error(null, e);
					break loop;
				}
				end = System.currentTimeMillis();
				logger.info("[" + threadId + "] sleep time [" + (end - begin) + "].");
				long loopEnd = System.currentTimeMillis();
				logger.info("[" + threadId + "] All time is [" + (loopEnd - loopBegin) + "].");
			}
			logger.info("Crawler thread[" + threadId + "] quits.");
		}
	}
}
