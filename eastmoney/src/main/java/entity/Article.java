package entity;

import java.util.ArrayList;
import java.util.List;

public class Article {

	// 文章标题
	private String title;

	// 发布时间
	private String publishTime;

	// 总页数
	private int totalPages;

	// 评论数
	private int totalComment;

	// 第一页的url
	private String firstPageUrl;

	// 文章作者
	private String author;

	// 贴吧名
	private String barName;

	// 文章内容
	private String article;

	private List<Comment> comments = new ArrayList<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPublishTime() {
		return publishTime;
	}

	public void setPublishTime(String publishTime) {
		this.publishTime = publishTime;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public int getTotalComment() {
		return totalComment;
	}

	public void setTotalComment(int totalComment) {
		this.totalComment = totalComment;
	}

	public String getFirstPageUrl() {
		return firstPageUrl;
	}

	public void setFirstPageUrl(String firstPageUrl) {
		this.firstPageUrl = firstPageUrl;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getBarName() {
		return barName;
	}

	public void setBarName(String barName) {
		this.barName = barName;
	}

	public String getArticle() {
		return article;
	}

	public void setArticle(String article) {
		this.article = article;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void addComments(List<Comment> comments) {
		this.comments.addAll(comments);
	}
}
