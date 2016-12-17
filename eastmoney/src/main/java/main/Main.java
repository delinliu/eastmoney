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

		int listThreadAmount = 1; // 单线程
		int listSleepSecond = 60; // 每次爬虫间隔60秒钟
		int listTimeoutSecond = 30; // 超时30秒钟

		int articleThreadAmount = 15; // 15线程
		int articleSleepSecond = 1; // 每次爬虫间隔1秒钟
		int articleWaitSecond = 10; // 超时10秒钟
		int articleTimeoutSecond = 60; // 当没有文章可爬的时候暂停60秒钟

		for (String arg : args) {
			if (!arg.matches("[a-zA-Z]+=[0-9]+")) {
				continue;
			}
			int value = Integer.parseInt(arg.split("=")[1]);
			if (arg.matches("listThreadAmount=([0-9]+)")) {
				listThreadAmount = value;
			} else if (arg.matches("listSleepSecond=([0-9]+)")) {
				listSleepSecond = value;
			} else if (arg.matches("listTimeoutSecond=([0-9]+)")) {
				listTimeoutSecond = value;
			} else if (arg.matches("articleThreadAmount=([0-9]+)")) {
				articleThreadAmount = value;
			} else if (arg.matches("articleSleepSecond=([0-9]+)")) {
				articleSleepSecond = value;
			} else if (arg.matches("articleWaitSecond=([0-9]+)")) {
				articleWaitSecond = value;
			} else if (arg.matches("articleTimeoutSecond=([0-9]+)")) {
				articleTimeoutSecond = value;
			}
		}

		listCrawler.start(listThreadAmount, listSleepSecond, listTimeoutSecond);
		articleCrawler.start(articleThreadAmount, articleSleepSecond, articleWaitSecond, articleTimeoutSecond);
	}
}
