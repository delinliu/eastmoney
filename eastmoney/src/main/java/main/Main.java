package main;

import crawler.ArticleCrawler;
import crawler.ArticleListCrawler;
import record.Record;
import record.RecordException;

public class Main {

	public static void main(String[] args) throws RecordException {

		Record record = new Record();
		ArticleListCrawler listCrawler = new ArticleListCrawler(record);
		ArticleCrawler articleCrawler = new ArticleCrawler(record);

		// 文章列表爬虫，单线程，每次爬虫间隔60秒钟，超时30秒钟
		listCrawler.start(1, 60, 30);

		// 文章爬虫，5线程，每次爬虫间隔1秒钟，超时10秒钟，当没有文章可爬的时候暂停60秒钟
		articleCrawler.start(5, 1, 60, 10);
	}
}
