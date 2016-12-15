package record;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import entity.Article;
import entity.Comment;

public class Record implements UrlProvider, UrlReceiver {

	// 记录已经爬过的url
	private Set<String> crawledSet;
	private String crawledFile = "crawled.log";

	// 记录还没有爬过的url
	private List<String> crawlingList;
	private int crawlingPos = 0; // crawlingList的下标
	private String crawlingFile = "crawling.log";

	// 爬过了，但是发现被删除了的url
	private Set<String> removedSet;
	private String removedFile = "removed.log";

	// page是列表页面已经爬到多少页了
	// file是存储数据到了多少个文件了
	private String meta = "meta.log";
	private AtomicInteger page;
	private AtomicInteger file;

	private String logFolder = "";
	private String dataFolder = "";

	private long dataSize = 1024 * 1024;

	public void setDataSize(long dataSize) {
		if (dataSize > 0) {
			this.dataSize = dataSize;
		}
	}

	public int getPageAmount() {
		return page.get();
	}

	public int getFileAmount() {
		return file.get();
	}

	public Record(String logFolder, String dataFolder) {
		this.logFolder = logFolder;
		this.dataFolder = dataFolder;
		createFolders();
		updateLogPath();
		init();
	}

	private void updateLogPath() {
		if (!logFolder.isEmpty()) {
			crawledFile = logFolder + "/" + crawledFile;
			crawlingFile = logFolder + "/" + crawlingFile;
			removedFile = logFolder + "/" + removedFile;
			meta = logFolder + "/" + meta;
		}
	}

	private void createFolders() {
		mkdirs(logFolder);
		mkdirs(dataFolder);
	}

	private void mkdirs(String folder) {
		File file = new File(folder);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public Record() {
		this("run/log", "run/data");
	}

	private void init() {
		loadCrawledSet();
		loadRemovedSet();
		loadCrawlingList();
		loadMeta();
		System.out.println("Crawling list size is " + crawlingList.size());
		System.out.println("Crawled set size is " + crawledSet.size());
		System.out.println("Page is now " + page.get());
		System.out.println("File is now " + file.get());
	}

	public String nextUrl() throws RecordException {
		synchronized (crawlingList) {
			synchronized (crawledSet) {
				String url = null;
				while (crawlingPos < crawlingList.size()
						&& crawledSet.contains((url = crawlingList.get(crawlingPos++)))) {
				}
				if (url != null && !crawledSet.contains(url)) {
					return url;
				} else {
					throw new RecordException("No more url.");
				}
			}
		}
	}

	public void addRemovedUrl(String url) throws IOException {
		synchronized (removedSet) {
			removedSet.add(url);
		}
		synchronized (removedFile) {
			RandomAccessFile raf = new RandomAccessFile(removedFile, "rw");
			raf.seek(raf.length());
			raf.write((url + "\n").getBytes());
			raf.close();
		}
	}

	/**
	 * 添加一个页面的url
	 * 
	 * @param urls
	 * @throws IOException
	 */
	public void addOnePageList(Set<String> urls) throws IOException {
		synchronized (crawledSet) {
			urls.removeAll(crawledSet);
		}
		synchronized (removedSet) {
			urls.removeAll(removedSet);
		}

		if (!urls.isEmpty()) {

			// 更新meta
			page.incrementAndGet();
			saveMeta();

			// 更新crawlingList
			synchronized (crawlingList) {
				crawlingList.addAll(urls);
			}

			// 更新crawlingFile
			synchronized (crawlingFile) {
				RandomAccessFile raf = new RandomAccessFile(crawlingFile, "rw");
				raf.seek(raf.length());
				StringBuilder buffer = new StringBuilder();
				for (String url : urls) {
					buffer.append(url).append("\n");
				}
				raf.write(buffer.toString().getBytes());
				raf.close();
			}
		}
	}

	private String getFileName() {
		int seq = file.get();
		String name = "" + seq;
		while (name.length() < 5) {
			name = "0" + name;
		}
		if (!dataFolder.isEmpty()) {
			name = dataFolder + "/" + name;
		}
		return name;
	}

	/**
	 * 保存一篇文章（包括评论）
	 * 
	 * @param url
	 * @param article
	 * @throws IOException
	 */
	public void addArticle(String url, Article article) throws IOException {

		synchronized (crawledSet) {
			// 如果已经爬虫过了，则不再添加
			if (crawledSet.contains(url)) {
				return;
			}
		}

		RandomAccessFile raf = new RandomAccessFile(getFileName(), "rw");
		long length = raf.length();
		if (length > dataSize) {
			raf.close();
			file.incrementAndGet();
			saveMeta();
			addArticle(url, article);
		} else {

			// 保存文章
			raf.seek(length);
			raf.write(article2String(url, article).getBytes());
			raf.close();

			// 更新crawledSet，更新crawledFile
			addCrawledUrl(url);
		}
	}

	private void addCrawledUrl(String url) throws IOException {
		synchronized (crawledSet) {
			crawledSet.add(url);
		}
		synchronized (crawledFile) {
			RandomAccessFile raf = new RandomAccessFile(crawledFile, "rw");
			long length = raf.length();
			raf.seek(length);
			raf.write((url + "\n").getBytes());
			raf.close();
		}
	}

	private String article2String(String url, Article article) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Begin============").append(article.getFirstPageUrl()).append("============").append("\n");
		buffer.append("[title]:").append(article.getTitle()).append("\n");
		buffer.append("[author]:").append(article.getAuthor()).append("\n");
		buffer.append("[publish time]:").append(article.getPublishTime()).append("\n");
		buffer.append("[bar name]:").append(article.getBarName()).append("\n");
		buffer.append("[article]:").append("\n");
		buffer.append(article.getArticle()).append("\n");
		buffer.append("[comments]: all " + article.getTotalComment() + ", crawled " + article.getComments().size())
				.append("\n");
		int seq = 1;
		for (Comment comment : article.getComments()) {
			buffer.append("[comment " + seq++ + "]").append("\n");
			buffer.append("[comment author]:").append(comment.getAuthor()).append("\n");
			buffer.append("[comment publish time]:").append(comment.getPublishTime()).append("\n");
			buffer.append("[comment]:").append(comment.getComment()).append("\n");
		}
		buffer.append("End============").append(article.getFirstPageUrl()).append("============").append("\n\n");
		return buffer.toString();
	}

	private void saveMeta() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(meta));
			writer.write("page=" + page + "\r\n");
			writer.write("file=" + file + "\r\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadMeta() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(meta));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.matches("page=[0-9]+")) {
					page = new AtomicInteger(Integer.parseInt(line.split("=")[1]));
				} else if (line.matches("file=[0-9]+")) {
					file = new AtomicInteger(Integer.parseInt(line.split("=")[1]));
				}
			}
			reader.close();
		} catch (IOException e) {
			page = new AtomicInteger(1);
			file = new AtomicInteger(1);
		}
	}

	private void loadCrawledSet() {
		crawledSet = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(crawledFile));
			String line;
			while ((line = reader.readLine()) != null) {
				crawledSet.add(line);
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Cannot load crawled set, because: " + e.getMessage());
		}
	}

	private void loadRemovedSet() {
		removedSet = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(removedFile));
			String line;
			while ((line = reader.readLine()) != null) {
				removedSet.add(line);
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Cannot load removed set, because: " + e.getMessage());
		}
	}

	private void loadCrawlingList() {
		crawlingList = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(crawlingFile));
			String line;
			while ((line = reader.readLine()) != null) {
				// 成果爬虫过了、已经被删除了的url不需要放进待爬虫列表中
				if (!crawledSet.contains(line) && !removedSet.contains(line)) {
					crawlingList.add(line);
				}
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Cannot load crawling set, because: " + e.getMessage());
		}
	}
}
