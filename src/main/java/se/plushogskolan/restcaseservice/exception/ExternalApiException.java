package se.plushogskolan.restcaseservice.exception;

public final class ExternalApiException extends Exception {

	private static final long serialVersionUID = 7263810700871285771L;

	public ExternalApiException(String message) {
		super(message);
	}

	public ExternalApiException(String message, Exception e) {
		super(message, e);
	}

}
