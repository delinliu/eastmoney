package parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import entity.Article;
import entity.Comment;
import util.Util;

public class ArticleParserTest {

	// http://guba.eastmoney.com/news,601069,164123294.html
	static final String articlePath = "src/test/resources/article.html";

	@Test
	public void test() throws Exception {
		String pageContent = Util.readFile(articlePath);
		ArticleParserInterface parser = new ArticleParser();
		Article article = parser.parseArticle(pageContent);
		Assert.assertNotNull(article);
		Assert.assertNotNull(article.getArticle());
		Assert.assertNull(article.getFirstPageUrl());
		Assert.assertEquals("田田来拿钱", article.getAuthor());
		Assert.assertEquals(242, article.getTotalComment());
		Assert.assertEquals("2015-05-08 09:47:29", article.getPublishTime());
		Assert.assertEquals(9, article.getTotalPages());
		Assert.assertEquals("田田大幅买入西部黄金。", article.getTitle());
		Assert.assertEquals("西部黄金吧", article.getBarName());

		List<Comment> comments = parser.parseComments(pageContent);
		article.setComments(comments);
		Assert.assertEquals(30, article.getComments().size());
		Comment firstComment = article.getComments().get(0);
		Assert.assertEquals("小田田终于等到机会来炒底了，这次看准了吗", firstComment.getComment());
		Assert.assertEquals("2015-05-08 10:07:42", firstComment.getPublishTime());
		Assert.assertEquals("低调3444", firstComment.getAuthor());
		Comment lastComment = article.getComments().get(29);
		Assert.assertEquals("yb547196727", lastComment.getAuthor());
		Assert.assertEquals("2015-05-08 20:37:34", lastComment.getPublishTime());
		Assert.assertEquals("田兄：你对4.29那个涨停板怎么看？我看不像是一路人做的。是不是拉板的人被撂倒了？", lastComment.getComment());

	}
}
