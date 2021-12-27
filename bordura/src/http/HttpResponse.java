package http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpResponse {
		
	public static final Map<Integer, String> statusCodes = Map.ofEntries(
		Map.entry(100, "Continue"),
		Map.entry(101, "Switching Protocols"),
		Map.entry(200, "OK"),
		Map.entry(201, "Created"),
		Map.entry(202, "Accepted"),
		Map.entry(203, "Non-Authoritative Information"),
		Map.entry(204, "No Content"),
		Map.entry(205, "Reset Content"),
		Map.entry(206, "Partial Content"),
		Map.entry(300, "Multiple Choices"),
		Map.entry(301, "Moved Permanently"),
		Map.entry(302, "Found"),
		Map.entry(303, "See Other"),
		Map.entry(304, "Not Modified"),
		Map.entry(305, "Use Proxy"),
		Map.entry(307, "Temporary Redirect"),
		Map.entry(400, "Bad Request"),
		Map.entry(401, "Unauthorized"),
		Map.entry(402, "Payment Required"),
		Map.entry(403, "Forbidden"),
		Map.entry(404, "Not Found"),
		Map.entry(405, "Method Not Allowed"),
		Map.entry(406, "Not Acceptable"),
		Map.entry(407, "Proxy Authentication Required"),
		Map.entry(408, "Request Timeout"),
		Map.entry(409, "Conflict"),
		Map.entry(410, "Gone"),
		Map.entry(411, "Length Required"),
		Map.entry(412, "Precondition Failed"),
		Map.entry(413, "Payload Too Large"),
		Map.entry(414, "URI Too Long"),
		Map.entry(415, "Unsupported Media Type"),
		Map.entry(416, "Range Not Satisfiable"),
		Map.entry(417, "Expectation Failed"),
		Map.entry(426, "Upgrade Required"),
		Map.entry(500, "Internal Server Error"),
		Map.entry(501, "Not Implemented"),
		Map.entry(502, "Bad Gateway"),
		Map.entry(503, "Service Unavailable"),
		Map.entry(504, "Gateway Timeout"),
		Map.entry(505, "HTTP Version Not Supported")
	);
	
	private final HttpVersion version;
	private final int status;
	private final List<Map.Entry<String, String>> headers;
	private final BodySource bodySource;
	
	private HttpResponse(HttpVersion version, int status, List<Map.Entry<String, String>> headers,
						 BodySource bodySource) {
		this.version = version;
		this.status = status;
		this.headers = List.copyOf(headers); // create unmodifiable list
		this.bodySource = bodySource;
	}
	
	/*
		Optional (if needed): Create method to convert the
		list of header fields into an unmodifiable map.
	*/
	
	public Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private HttpVersion version;
		private int status;
		private List<Map.Entry<String, String>> headers;
		private BodySource bodySource;
		
		private Builder() {
			this.version = HttpVersion.HTTP_1_1;
			this.status = 500;
			this.headers = new LinkedList<Map.Entry<String, String>>();
			this.bodySource = new NoBodySource();
		}
		
		public Builder version(HttpVersion version) {
			this.version = version;
			return this;
		}
		
		public Builder status(int status) {
			if (statusCodes.containsKey(status)) {
				this.status = status;
				return this;
			} else {
				throw new IllegalArgumentException("Invalid status code: " + Integer.toString(status, 10));
			}
		}
		
		public Builder addHeader(String name, String value) {
			this.headers.add(Map.entry(name, value));
			return this;
		}
		
		public Builder bodySource(Path filePath) {
			this.bodySource = new FileBodySource(filePath);
			return this;
		}
		
		public Builder bodySource(byte[] content) {
			this.bodySource = new ByteArrayBodySource(content);
			return this;
		}
		
		public HttpResponse build() {
			return new HttpResponse(this.version, this.status, this.headers,
					this.bodySource);
		}
	}
	
	private static sealed interface BodySource
		permits ByteArrayBodySource, FileBodySource, NoBodySource {
			void writeTo(OutputStream out) throws IOException;
	}
	
	private record ByteArrayBodySource(byte[] content) implements BodySource {
		public void writeTo(OutputStream out) throws IOException {
			for (int i = 0; i < content.length; i++) {
				out.write(content[i]);
			}
		}
	}
	
	private record FileBodySource(Path filePath) implements BodySource {
		public void writeTo(OutputStream out) {
			// TODO
		}
	}
	
	private record NoBodySource() implements BodySource {
		public void writeTo(OutputStream out) {
			// Do nothing.
		}
	}

}
