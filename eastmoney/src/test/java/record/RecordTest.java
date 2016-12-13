package record;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import entity.Article;
import entity.Comment;
import parser.ArticleParser;
import util.Util;

public class RecordTest {

	// http://guba.eastmoney.com/news,601069,164123294.html
	static final String articlePath = "src/test/resources/article.html";

	@Test
	public void test() throws Exception {

		deleteFolder(new File("test"));

		String pageContent = Util.readFile(articlePath);
		ArticleParser parser = new ArticleParser();
		Article article = parser.parseArticle(pageContent);
		List<Comment> comments = parser.parseComments(pageContent);
		article.setComments(comments);
		Record record = new Record("test/log", "test/data");
		record.setDataSize(7 * 1024);
		record.addArticle("test1.html", article);
		record.addArticle("test2.html", article);
		record.addArticle("test3.html", article);
		Assert.assertEquals(1, record.getPageAmount());
		Assert.assertEquals(2, record.getFileAmount());

		record = new Record("test/log", "test/data");
		Assert.assertEquals(1, record.getPageAmount());
		Assert.assertEquals(2, record.getFileAmount());
		record.setDataSize(7 * 1024);
		record.addArticle("test1.html", article);
		record.addArticle("test2.html", article);
		record.addArticle("test3.html", article);
		Assert.assertEquals(1, record.getPageAmount());
		Assert.assertEquals(3, record.getFileAmount());
		assertRecordNoNextUrl(record);

		record.addOnePageList(new HashSet<String>(Arrays.asList(new String[] { "t1.html", "t2.html", "t3.html" })));
		Assert.assertEquals(2, record.getPageAmount());
		List<String> list = new ArrayList<>();
		for (int i = 1; i <= 3; i++) {
			list.add(record.nextUrl());
		}
		Collections.sort(list);
		for (int i = 1, o = 0; i <= 3; i++, o++) {
			Assert.assertEquals("t" + i + ".html", list.get(o));
		}
		assertRecordNoNextUrl(record);

		Set<String> urls = new HashSet<String>();
		urls.add("test4.html");
		urls.add("test5.html");
		urls.add("test6.html");
		record.addOnePageList(urls);
		list.clear();
		for (int i = 4; i <= 6; i++) {
			list.add(record.nextUrl());
		}
		Collections.sort(list);
		for (int i = 4, o = 0; i <= 6; i++, o++) {
			Assert.assertEquals("test" + i + ".html", list.get(o));
		}
		assertRecordNoNextUrl(record);

		Assert.assertEquals(3, record.getPageAmount());
		record.addArticle("test4.html", article);
		record.addArticle("test5.html", article);
		record.addArticle("test6.html", article);
		record.addOnePageList(urls);
		Assert.assertEquals(3, record.getPageAmount());
		Assert.assertEquals(5, record.getFileAmount());

		record = new Record("test/log", "test/data");
		Assert.assertEquals(3, record.getPageAmount());
		Assert.assertEquals(5, record.getFileAmount());

	}

	private void assertRecordNoNextUrl(Record record) {
		try {
			record.nextUrl();
			Assert.assertTrue(false);
		} catch (RecordException e) {
			// empty
		}
	}

	private void deleteFolder(File folder) {
		if (folder.isFile()) {
			folder.delete();
		} else if (folder.isDirectory()) {
			for (File sub : folder.listFiles()) {
				deleteFolder(sub);
			}
			folder.delete();
		}
	}
}
