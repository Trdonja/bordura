package http;

import java.nio.charset.StandardCharsets;

public enum HttpVersion {

	HTTP_1_0 ("HTTP/1.0"),
	HTTP_1_1 ("HTTP/1.1"),
	HTTP_2 ("HTTP/2");
	
	private final String displayString;
	private final byte[] charBytes;
	
	private HttpVersion(String display) {
		this.displayString = display;
		this.charBytes = display.getBytes(StandardCharsets.US_ASCII);
	}
	
	@Override
	public String toString() {
		return this.displayString;
	}
	
	public byte[] charBytes() {
		return this.charBytes;
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
