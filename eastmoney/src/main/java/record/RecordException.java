package record;

public class RecordException extends Exception {

	private static final long serialVersionUID = 1316798569212329270L;
	private String baseMessage;

	public RecordException(String message) {
		super(message);
		this.baseMessage = message;
	}

	public RecordException(String message, String url) {
		super(message + " [url=" + url + "]");
		this.baseMessage = message;
	}

	public String getBaseMessage() {
		return baseMessage;
	}
}
