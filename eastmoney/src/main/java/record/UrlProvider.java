package record;

import java.io.IOException;

import entity.Article;

public interface UrlProvider {
	String nextUrl() throws RecordException;

	void addRemovedUrl(String url) throws IOException;

	void addArticle(String url, Article article) throws IOException;
}
