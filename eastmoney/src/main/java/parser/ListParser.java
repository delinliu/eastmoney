package parser;

import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ListParser implements ListParserInterface {

	@Override
	public Set<String> parseList(String pageContent) throws ParserException {
		Set<String> urlSet = new HashSet<>();
		String field = "[Parse List] ";
		Document document = Jsoup.parse(pageContent);
		Elements elements = document.getElementsByClass("newlist");
		if (elements.size() != 1) {
			throw new ParserException(field + "Class newlist is not 1.");
		}
		Elements subs = elements.get(0).getElementsByClass("sub");
		if (subs.isEmpty()) {
			throw new ParserException(field + "Class sub is 0.");
		}
		for (Element sub : subs) {
			Elements as = sub.getElementsByTag("a");
			for (Element a : as) {
				String href = a.attr("href");
				if (href.matches("/news.*html")) {
					urlSet.add(href);
				}
			}
		}
		return urlSet;
	}

}
