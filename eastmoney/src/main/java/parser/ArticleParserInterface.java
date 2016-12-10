package parser;

import java.util.List;

import entity.Article;
import entity.Comment;

public interface ArticleParserInterface {

	Article parseArticle(String pageContent) throws ParserException;

	List<Comment> parseComments(String pageContent) throws ParserException;
}
