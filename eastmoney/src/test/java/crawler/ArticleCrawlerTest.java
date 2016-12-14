package crawler;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import record.Record;
import util.Util;

public class ArticleCrawlerTest {

	@Test
	public void test() throws Exception {
		Util.deleteFolder(new File("test"));
		Record record = new Record("test/log", "test/data");
		record.setDataSize(1024 * 10); // 10KB per file
		record.addOnePageList(new HashSet<String>(
				Arrays.asList(new String[] { "/news,cjpl,579006595.html", "/news,300066,578898197.html" })));
		ArticleCrawler crawler = new ArticleCrawler(record);
		int crawlTime = 10;
		System.out.println("Wait " + crawlTime + "s for article crawling tests.");
		crawler.start(1, 1, 60000, 5000);
		Thread.sleep(1000 * crawlTime);
		crawler.shutdown();
		Thread.sleep(2000);
	}
}
