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
	static final String articleNoCommentPath = "src/test/resources/article-no-comments.html";
	static final String article404Path = "src/test/resources/article-404.html";
	static final String articleNoTitlePath = "src/test/resources/article-no-title.html";
	static final String articleQa = "src/test/resources/article-qa.html";

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
		article.addComments(comments);
		Assert.assertEquals(30, article.getComments().size());
		Comment firstComment = article.getComments().get(0);
		Assert.assertEquals("小田田终于等到机会来炒底了，这次看准了吗", firstComment.getComment());
		Assert.assertEquals("2015-05-08 10:07:42", firstComment.getPublishTime());
		Assert.assertEquals("低调3444", firstComment.getAuthor());
		Comment lastComment = article.getComments().get(29);
		Assert.assertEquals("yb547196727", lastComment.getAuthor());
		Assert.assertEquals("2015-05-08 20:37:34", lastComment.getPublishTime());
		Assert.assertEquals("田兄：你对4.29那个涨停板怎么看？我看不像是一路人做的。是不是拉板的人被撂倒了？", lastComment.getComment());

		pageContent = Util.readFile(articleNoCommentPath);
		article = parser.parseArticle(pageContent);
		Assert.assertEquals(0, article.getTotalComment());
		Assert.assertEquals(0, article.getTotalPages());

		pageContent = Util.readFile(article404Path);
		try {
			parser.parseArticle(pageContent);
			Assert.assertTrue(false);
		} catch (ParserException e) {
			Assert.assertEquals("Article removed.", e.getMessage());
		}

		pageContent = Util.readFile(articleNoTitlePath);
		parser.parseArticle(pageContent);

		pageContent = Util.readFile(articleQa);
		article = parser.parseArticle(pageContent);
		Assert.assertEquals(
				"请问公司，闲置资金购买理财，而不增持本公司股票，是不是对格林美股票未来走势没有信心？\n格林美： 公司购买理财产品的资金分别为未到使用期的定增募集资金股权转让款和部分闲置的自有资金，有利于提高公司资金的利用率，降低财务成本。",
				article.getArticle());
	}
}
