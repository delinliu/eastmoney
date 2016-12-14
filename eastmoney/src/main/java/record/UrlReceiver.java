package record;

import java.io.IOException;
import java.util.Set;

public interface UrlReceiver {
	void addOnePageList(Set<String> urls) throws IOException;
}
