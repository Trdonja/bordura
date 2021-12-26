package http;

public enum HttpVersion {

	HTTP_1_0 ("HTTP/1.0"),
	HTTP_1_1 ("HTTP/1.1"),
	HTTP_2 ("HTTP/2");
	
	private final String version;
	
	private HttpVersion(String display) {
		this.version = display;
	}
	
	@Override
	public String toString() {
		return this.version;
	}
	
	public static HttpVersion get(int majorVersion, int minorVersion) throws IllegalArgumentException {
		if (majorVersion == 1) {
			if (minorVersion == 1) {
				return HTTP_1_1;
			} else if (minorVersion == 0) {
				return HTTP_1_0;
			} else {
				throw new IllegalArgumentException("Unknown HTTP version " + majorVersion + "." + minorVersion);
			}
		} else if (majorVersion == 2) {
			if (minorVersion == 0) {
				return HTTP_2;
			} else {
				throw new IllegalArgumentException("Unknown HTTP version " + majorVersion + "." + minorVersion);
			}
		} else {
			throw new IllegalArgumentException("Unknown HTTP version " + majorVersion + "." + minorVersion);
		}
	}
	
}
