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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import entity.Article;
import entity.Comment;

public class Record implements UrlProvider, UrlReceiver {

	private Logger logger = LoggerFactory.getLogger(Record.class);

	// 记录已经爬过的url
	private Set<String> crawledSet = new HashSet<>();
	private String crawledFile = "crawled.log";

	// 记录还没有爬过的url
	private List<String> crawlingList = new ArrayList<>();
	private int crawlingPos = 0; // crawlingList的下标
	private String crawlingFile = "crawling.log";

	// 记录文件中的爬虫位置
	private AtomicInteger crawlingRecord = new AtomicInteger(0);
	private String crawlingRecordFile = "pos.log";
	private int loadTimeLoadCrawlingSize = 0;

	// 爬过了，但是发现被删除了的url
	private Set<String> removedSet = new HashSet<>();
	private String removedFile = "removed.log";

	// 临时的set，不会保存到磁盘中，用它来保存正在爬虫的url，防止多个线程同时爬一个url
	private Set<String> tmpSet = new HashSet<>();

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
			crawlingRecordFile = logFolder + "/" + crawlingRecordFile;
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
		initListAndSet();
		loadMeta();
		loadCrawlingRecord();
		logger.info("Crawling list size is " + crawlingList.size());
		logger.info("Crawled set size is " + crawledSet.size());
		logger.info("Page is now " + page.get());
		logger.info("File is now " + file.get());
		logger.info("Crawling record is now " + crawlingRecord.get());
	}

	// 初始化完毕后，crawlingList和crawledSet的size大致在这个数字
	private int initCrawlingSize = 30000;
	private int initCrawledSize = 3000;

	// 初始化时从文件中读取的数目为这个数值
	private int loadSize = 70000;

	private void initListAndSet() {

		// 锁住所有变量（除了meta）
		synchronized (crawlingList) {
			synchronized (articleBuffer) {
				synchronized (crawledSet) {
					synchronized (crawledFile) {
						synchronized (crawlingFile) {
							synchronized (removedFile) {

								crawlingRecord.addAndGet(loadTimeLoadCrawlingSize);
								saveCrawlingRecord();

								// 加载3个list
								List<String> crawled = loadListFromEnd(crawledFile, loadSize);
								List<String> removed = loadList(removedFile, 0, loadSize);
								List<String> crawling = loadList(crawlingFile, crawlingRecord.get(), loadSize);
								logger.info("Loaded crawled, removed and crawling.");

								// crawled转成set，并且把缓存中的数据也加上
								Set<String> crawledSet = new HashSet<>(crawled);
								addUrlBufferToSet(crawledSet, crawledUrlBuffer);
								crawledSet.addAll(tmpSet);

								// removed转成set，并且把缓存中的数据也加上
								Set<String> removedSet = new HashSet<>(removed);
								addUrlBufferToSet(removedSet, removedUrlBuffer);

								// 把crawling中已经爬虫过的url删除掉放入crawlingList中，并且其size有上限
								int thisTimeLoadCrawlingSize = 0;
								List<String> crawlingList = new ArrayList<>(initCrawlingSize);
								for (String url : crawling) {
									if (!crawledSet.contains(url) && !removedSet.contains(url)) {
										crawlingList.add(url);
										if (crawlingList.size() >= initCrawlingSize) {
											break;
										}
									}
									thisTimeLoadCrawlingSize++;
								}
								loadTimeLoadCrawlingSize = thisTimeLoadCrawlingSize;

								// 重置crawlingList
								this.crawlingList.clear();
								this.crawlingList.addAll(crawlingList);
								crawlingPos = 0;

								// 重置crawledSet，size有上限
								this.crawledSet.clear();
								for (int i = crawled.size() - 1; i >= 0
										&& this.crawledSet.size() < initCrawledSize; i--) {
									this.crawledSet.add(crawled.get(i));
								}
								addUrlBufferToSet(this.crawledSet, crawledUrlBuffer);

								// 重置removedSet
								this.removedSet.clear();
								this.removedSet.addAll(removedSet);

								logger.info("Init List and Set successful, crawling[" + crawling.size()
										+ "] crawlingList[" + this.crawlingList.size() + "] crawled[" + crawled.size()
										+ "] crawledSet[" + this.crawledSet.size() + "] removed[" + removed.size()
										+ "] removedSet[" + this.removedSet.size() + "] thisTimeLoadCrawlingSize["
										+ thisTimeLoadCrawlingSize + "]");
							}
						}
					}
				}
			}
		}
	}

	private void addUrlBufferToSet(Set<String> set, StringBuilder buffer) {
		for (String url : buffer.toString().split("\n")) {
			if (!url.isEmpty()) {
				set.add(url);
			}
		}
	}

	public String nextUrl() throws RecordException {
		return nextUrlHelper(1);
	}

	private String nextUrlHelper(int tryAmount) throws RecordException {
		synchronized (crawlingList) {
			synchronized (crawledSet) {
				String url = null;
				while (crawlingPos < crawlingList.size()) {
					url = crawlingList.get(crawlingPos++);
					if (!crawledSet.contains(url) && !tmpSet.contains(url)) {
						break;
					}
				}
				if (url != null && !crawledSet.contains(url)) {
					tmpSet.add(url);
					return url;
				} else {
					if (tryAmount <= 0) {
						throw new RecordException("No more url.");
					} else {
						initListAndSet();
						return nextUrlHelper(tryAmount - 1);
					}
				}
			}
		}
	}

	private StringBuilder removedUrlBuffer = new StringBuilder();

	/**
	 * 添加一个removed url到缓存
	 */
	public void addRemovedUrl(String url) {
		synchronized (removedSet) {
			removedSet.add(url);
		}
		synchronized (removedFile) {
			removedUrlBuffer.append(url).append("\n");
		}
	}

	/**
	 * 把removed url刷入磁盘
	 * 
	 * @throws IOException
	 */
	private void flushRemovedUrl() throws IOException {
		synchronized (removedFile) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(removedFile, true));
			writer.append(removedUrlBuffer);
			writer.close();
			removedUrlBuffer.delete(0, removedUrlBuffer.length());
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
			// synchronized (crawlingList) {
			// crawlingList.addAll(urls);
			// }

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

	private StringBuilder articleBuffer = new StringBuilder();

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
			tmpSet.remove(url);
		}

		String content = article2String(url, article);
		synchronized (articleBuffer) {
			articleBuffer.append(content).append("\n");
			addCrawledUrl(url);
			if (articleBuffer.length() > dataSize) {

				// 写入文章文件
				BufferedWriter writer = new BufferedWriter(new FileWriter(getFileName(), true));
				writer.write(articleBuffer.toString());
				writer.close();
				articleBuffer.delete(0, articleBuffer.length());

				// 写入已经爬好的url
				flushCrawledUrl();

				// 写入爬虫失败的url
				flushRemovedUrl();

				file.incrementAndGet();

				// 写入meta
				saveMeta();
			}
		}
	}

	private StringBuilder crawledUrlBuffer = new StringBuilder();

	private void addCrawledUrl(String url) throws IOException {
		synchronized (crawledSet) {
			crawledSet.add(url);
		}
		synchronized (crawledFile) {
			crawledUrlBuffer.append(url).append("\n");
		}
	}

	private void flushCrawledUrl() throws IOException {
		synchronized (crawledFile) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(crawledFile, true));
			writer.append(crawledUrlBuffer.toString());
			writer.close();
			crawledUrlBuffer.delete(0, crawledUrlBuffer.length());
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
		synchronized (meta) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(meta));
				writer.write("page=" + page + "\r\n");
				writer.write("file=" + file + "\r\n");
				writer.close();
			} catch (IOException e) {
				logger.error(null, e);
			}
		}
	}

	private void saveCrawlingRecord() {
		synchronized (crawlingRecordFile) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(crawlingRecordFile));
				writer.write("crawling=" + crawlingRecord + "\r\n");
				writer.close();
			} catch (IOException e) {
				logger.error(null, e);
			}
		}
	}

	private void loadCrawlingRecord() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(crawlingRecordFile));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.matches("crawling=[0-9]+")) {
					crawlingRecord.set(Integer.parseInt(line.split("=")[1]));
				}
			}
			reader.close();
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

	/**
	 * 读取文件构造成List。读取最后amount个。
	 * 
	 * @param file
	 * @param amount
	 * @return
	 */
	private List<String> loadListFromEnd(String file, int amount) {
		int totalAmount = lines(file);
		int skipAmount = totalAmount - amount;
		return loadList(file, skipAmount, amount);
	}

	private int lines(String file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int line = 0;
			while (reader.readLine() != null) {
				line++;
			}
			reader.close();
			return line;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 读取文件构造成List。从开头读起，忽略掉skip个，最多读amount个。
	 * 
	 * @param file
	 * @param skip
	 * @param amount
	 * @return
	 */
	private List<String> loadList(String file, int skip, int amount) {
		List<String> crawledList = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {

				// 忽略掉开头的skip个
				if (skip > 0) {
					skip--;
					continue;
				}

				// 最多加载amount个
				if (amount > 0) {
					crawledList.add(line);
					amount--;
					continue;
				}
				break;
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Cannot load list [" + file + "], because: " + e.getMessage());
		}
		return crawledList;
	}
}
