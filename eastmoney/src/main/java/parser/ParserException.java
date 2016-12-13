package parser;

public class ParserException extends Exception {

	private static final long serialVersionUID = 5072001714363127998L;
	private String baseMessage;

	public ParserException(String message) {
		super(message);
		this.baseMessage = message;
	}

	public ParserException(String message, String url) {
		super(message + " [url=" + url + "]");
		this.baseMessage = message;
	}

	public String getBaseMessage() {
		return baseMessage;
	}
}
