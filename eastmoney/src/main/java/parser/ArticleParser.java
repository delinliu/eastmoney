package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import entity.Article;
import entity.Comment;

public class ArticleParser implements ArticleParserInterface {

	@Override
	public Article parseArticle(String pageContent) throws ParserException {

		Document document = Jsoup.parse(pageContent);
		int totalComment = totalComment(document);
		int totalPages = (totalComment + 29) / 30;
		String title = title(document);
		String publishTime = publishTime(document);
		String author = author(document);
		String barName = barName(document);
		String articleContent = article(document);
		Article article = new Article();
		article.setTotalPages(totalPages);
		article.setTotalComment(totalComment);
		article.setTitle(title);
		article.setPublishTime(publishTime);
		article.setAuthor(author);
		article.setBarName(barName);
		article.setArticle(articleContent);
		return article;
	}

	@Override
	public List<Comment> parseComments(String pageContent) throws ParserException {
		String field = "[Comments] ";
		Document document = Jsoup.parse(pageContent);
		List<Comment> comments = new ArrayList<>();
		Element zwlist = document.getElementById("zwlist");
		checkUnique(zwlist, field + "Id zwlist is not 1.");
		Elements zwlis = zwlist.getElementsByClass("zwli");
		for (Element zwli : zwlis) {
			String author = getFirstFieldText(zwli, "zwlianame");
			String commentContent = getFirstFieldText(zwli, "zwlitext");
			String publishTime = getFirstFieldText(zwli, "zwlitime");
			Pattern p = Pattern.compile("发表于 *(\\d\\d\\d\\d-\\d\\d-\\d\\d) *(\\d\\d:\\d\\d:\\d\\d)");
			Matcher m = p.matcher(publishTime);
			if (m.find()) {
				publishTime = m.group(1) + " " + m.group(2);
			} else {
				publishTime = null;
			}
			Comment comment = new Comment();
			comment.setAuthor(author);
			comment.setComment(commentContent);
			comment.setPublishTime(publishTime);
			comments.add(comment);
		}
		return comments;
	}

	private String getFirstFieldText(Element element, String className) {
		Elements elements = element.getElementsByClass(className);
		if (!elements.isEmpty()) {
			return elements.get(0).text();
		}
		return "";
	}

	private void checkUnique(Element element, String errorMessage) throws ParserException {
		if (element == null) {
			throw new ParserException(errorMessage);
		}
	}

	private void checkUnique(Elements elements, String errorMessage) throws ParserException {
		if (elements == null || elements.isEmpty()) {
			throw new ParserException(errorMessage);
		}
	}

	private int totalComment(Document document) throws ParserException {
		String field = "[Total Comment] ";
		Element element = document.getElementById("zwcontab");
		
		// 没有这个id就是没有评论
		if(element == null) {
			return 0;
		}
		
		String text = element.text().replaceAll(" ", "");
		Pattern p = Pattern.compile("全部评论（([0-9]+)）");
		Matcher m = p.matcher(text);
		if (!m.find()) {
			throw new ParserException(field + "Cannot find total comment.");
		}
		return Integer.parseInt(m.group(1));
	}

	private String title(Document document) throws ParserException {
		String field = "[Title] ";
		Element element = document.getElementById("zwconttbt");
		checkUnique(element, field + "Id zwconttbt is not 1.");
		String title = element.text().trim();
		if (title.isEmpty()) {
			throw new ParserException(field + "Cannot find title.");
		}
		return title;
	}

	private String publishTime(Document document) throws ParserException {
		String field = "[Publish Time] ";
		Elements elements = document.getElementsByClass("zwfbtime");
		checkUnique(elements, field + "Class zwfbtime is not 1.");
		String text = elements.get(0).text();
		Pattern p = Pattern.compile("发表于 *(\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d)");
		Matcher m = p.matcher(text);
		if (!m.find()) {
			throw new ParserException(field + "Cannot find publish time.");
		}
		return m.group(1);
	}

	private String author(Document document) throws ParserException {
		String field = "[Author] ";
		Element element = document.getElementById("zwconttbn");
		checkUnique(element, field + "Id zwconttbn is not 1.");
		String author = element.text().trim();
		if (author.isEmpty()) {
			throw new ParserException(field + "Cannot find author.");
		}
		return author;
	}

	private String barName(Document document) throws ParserException {
		String field = "[Bar Name] ";
		Element element = document.getElementById("stockname");
		checkUnique(element, field + "Id stockname is not 1.");
		String barName = element.text().trim();
		if (barName.isEmpty()) {
			throw new ParserException(field + "Cannot find bar name.");
		}
		return barName;
	}

	private String article(Document document) throws ParserException {
		String field = "[Article] ";
		Element element = document.getElementById("zwconbody");
		checkUnique(element, field + "Id zwconbody is not 1.");
		String article = element.text().trim();
		if (article.isEmpty()) {
			throw new ParserException(field + "Cannot find article.");
		}
		return article;

	}
}
