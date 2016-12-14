package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

final public class Util {

	private Util() {
		// empty
	}

	public static String readFile(String path) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(path));
		StringBuilder content = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			content.append(line).append("\n");
		}
		reader.close();
		return content.toString();
	}

	public static void deleteFolder(File folder) {
		if (folder.isFile()) {
			folder.delete();
		} else if (folder.isDirectory()) {
			for (File sub : folder.listFiles()) {
				deleteFolder(sub);
			}
			folder.delete();
		}
	}

}
