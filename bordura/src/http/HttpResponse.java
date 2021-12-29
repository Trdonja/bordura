package http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
	private final BodyPublisher bodyPublisher;
	private final boolean useGzipTransferEncoding; // indicates if gzip Transfer-Encoding for body must be used
	
	private HttpResponse(HttpVersion version, int status, List<Map.Entry<String, String>> headers,
						 BodyPublisher bodyPublisher, boolean useGzipTransferEncoding) {
		this.version = version;
		this.status = status;
		this.headers = List.copyOf(headers); // create unmodifiable list
		this.bodyPublisher = bodyPublisher;
		this.useGzipTransferEncoding = useGzipTransferEncoding;
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
		private BodyPublisher bodyPub;
		private boolean gzipTE;
		
		private Builder() {
			this.version = HttpVersion.HTTP_1_1;
			this.status = 500;
			this.headers = new LinkedList<Map.Entry<String, String>>();
			this.bodyPub = BodyPublisher.noBody();
			this.gzipTE = false;
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
			// (1) Validate characters in String name
			for (int i = 0; i < name.length(); i++) {
				if (!HttpRequest.isTchar(name.charAt(i))) { // character is not Tchar
					throw new IllegalArgumentException("Invalid character in header field name.");
				}
			}
			// (2) Validate characters in string value
			for (int i = 0; i < value.length(); i++) {
				int c = value.charAt(i);
				if (c > 0x7E || (c < 0x20 && c != 0x09)) { // c is not VCHAR (visible, printing character)
					// nor SP (0x20) nor HTAB (0x09)
					throw new IllegalArgumentException("Invalid character in header field value.");
				}
			}
			// (3) Reject "Content-Length" and "Transfer-Encoding" headers,
			//     since they will be added automatically at build() method call as necessary.
			String nameLower = name.toLowerCase();
			if (nameLower.equals("content-length") || nameLower.equals("transfer-encoding")) {
				throw new IllegalArgumentException("""
				Content-Length and Transfer-Encoding header fields are added \
				automatically at build() time, if needed at all.""");
			}
			this.headers.add(Map.entry(name, value));
			return this;
		}
		
		public Builder body(BodyPublisher bodyPub) {
			this.bodyPub = bodyPub;
			return this;
		}
		
		public Builder useGzipTransferEncoding(boolean use) {
			this.gzipTE = use; // if true, it will be also chunked (a single chunk)
			return this;
		}
		
		public HttpResponse build() throws IOException { // throws if file size cannot be read
			// Add appropriate "Content-Length" and "Transfer-Encoding" headers, if body is present
			if (!(this.bodyPub instanceof NoBody)) {
				if (this.gzipTE) {
					this.headers.add(Map.entry("Transfer-Encoding", "gzip, chunked"));
				} else {
					this.headers.add(Map.entry("Content-Length", Long.toString(this.bodyPub.contentLength())));
				}
			}
			return new HttpResponse(this.version, this.status, this.headers,
					this.bodyPub, this.gzipTE);
		}
		
	}
	
	public static sealed interface BodyPublisher
		permits BodyPublisherOfByteArray,
				BodyPublisherOfFile,
				BodyPublisherOfString,
				NoBody
	{
			void writeTo(OutputStream out) throws IOException;
			
			long contentLength() throws IOException;
			
			static final NoBody NO_BODY = new NoBody();
			
			public static BodyPublisher ofByteArray(byte[] buf) {
				return new BodyPublisherOfByteArray(buf, 0, buf.length);
			}
			
			public static BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
				return new BodyPublisherOfByteArray(buf, offset, length);
			}
			
			public static BodyPublisher ofFile(Path path) {
				return new BodyPublisherOfFile(path);
			}
			
			public static BodyPublisher ofString(String s) {
				return new BodyPublisherOfString(s, StandardCharsets.UTF_8);
			}
			
			public static BodyPublisher ofString(String s, Charset charset) {
				return new BodyPublisherOfString(s, charset);
			}
			
			public static BodyPublisher noBody() {
				return NO_BODY;
			}
			
			/* More BodyPublishers might be implemented in the future.
			 * For inspiration, visit
			 * https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpRequest.BodyPublishers.html
			 */
	}
	
	private record BodyPublisherOfByteArray(byte[] buf, int offset, int length) implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			out.write(buf, offset, length);
			out.flush();
		}
		public long contentLength() {
			return length;
		}
	}
	
	private record BodyPublisherOfFile(Path path) implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			try (var fileStream = new BufferedInputStream(Files.newInputStream(path))) {
				fileStream.transferTo(out);
			}
			out.flush();
		}
		public long contentLength() throws IOException {
			return Files.size(path);
		}
	}
	
	private record BodyPublisherOfString(String s, Charset charset) implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			out.write(s.getBytes(charset));
			out.flush();
		}
		public long contentLength() {
			return (long) s.length();
		}
	}
	
	private record NoBody() implements BodyPublisher {
		public void writeTo(OutputStream out) {} // Do nothing.
		public long contentLength() {
			return 0L;
		}
	}

}
