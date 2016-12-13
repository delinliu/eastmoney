package parser;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import util.Util;

public class ListParserTest {

	// http://guba.eastmoney.com/default_2.html
	static final String articlePath = "src/test/resources/list.html";

	@Test
	public void test() throws Exception {
		String content = Util.readFile(articlePath);
		ListParserInterface parser = new ListParser();
		Set<String> urlSet = parser.parseList(content);
		Assert.assertEquals(52, urlSet.size());
		Assert.assertTrue(urlSet.contains("/news,002408,578887499.html"));
		Assert.assertTrue(urlSet.contains("/news,300066,578898197.html"));
		Assert.assertTrue(urlSet.contains("/news,yuanyou,576302435.html"));
	}
}
