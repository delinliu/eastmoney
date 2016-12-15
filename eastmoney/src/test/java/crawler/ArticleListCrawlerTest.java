package crawler;

import java.io.File;

import org.junit.Test;

import record.Record;
import util.Util;

public class ArticleListCrawlerTest {

	@Test
	public void test() throws Exception {

		Util.deleteFolder(new File("test"));
		Record record = new Record("test/log", "test/data");
		ArticleListCrawler crawler = new ArticleListCrawler(record);
		crawler.start(1, 1, 5);
		Thread.sleep(10000);
	}
}
