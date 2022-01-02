package http;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

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
	private final TransferEncoding transferEncoding;
	
	private HttpResponse(HttpVersion version, int status, List<Map.Entry<String, String>> headers,
						 BodyPublisher bodyPublisher, TransferEncoding transferEncoding) {
		this.version = version;
		this.status = status;
		this.headers = List.copyOf(headers); // create unmodifiable list
		this.bodyPublisher = bodyPublisher;
		this.transferEncoding = transferEncoding;
	}
	
	public void writeTo(OutputStream out) throws IOException {
		// write status line
		out.write(version.charBytes());
		out.write((byte) AsciiChars.SP);
		out.write(Integer.toString(status).getBytes(US_ASCII));
		out.write((byte) AsciiChars.SP);
		out.write(statusCodes.get(status).getBytes(US_ASCII));
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		// write headers
		var iter = headers.listIterator();
		while (iter.hasNext()) {
			var header = iter.next();
			out.write(header.getKey().getBytes(US_ASCII));
			out.write(AsciiChars.COL);
			out.write(AsciiChars.SP);
			out.write(header.getValue().getBytes(US_ASCII));
			out.write((byte) AsciiChars.CR);
			out.write((byte) AsciiChars.LF);
		}
		// write empty line
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		// write body, if necessary
		if (bodyPublisher instanceof NoBody) {
			if (transferEncoding == TransferEncoding.NONE_AND_CLOSE) {
				out.close();
			}
		} else {
			switch (transferEncoding) {
			case NONE:
				bodyPublisher.writeTo(out);
				out.flush();
				break;
			case NONE_AND_CLOSE:
				bodyPublisher.writeTo(out);
				out.flush();
				out.close();
				break;
			case CHUNKED:
				bodyPublisher.writeChunkedTo(out);
				break;
			case GZIP_AND_CHUNKED:
				bodyPublisher.writeGzippedTo(new ChunkedOutputStream(out, 1024));
				break;
			case GZIP_AND_CLOSE:
				bodyPublisher.writeGzippedTo(out); // will also close out
				break;
			}
		}
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
		private boolean gzipTransferEncoding;
		private boolean closeConnection;
		
		private Builder() {
			this.version = HttpVersion.HTTP_1_1;
			this.status = 500;
			this.headers = new LinkedList<Map.Entry<String, String>>();
			this.bodyPub = BodyPublisher.noBody();
			this.gzipTransferEncoding = false;
			this.closeConnection = false;
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
			// (1) Reject "Content-Length" and "Transfer-Encoding" headers,
			//     since they will be added automatically at build() method call as necessary.
			String nameLower = name.toLowerCase();
			if (nameLower.equals("content-length") || nameLower.equals("transfer-encoding")) {
				throw new IllegalArgumentException("""
				Content-Length and Transfer-Encoding header fields are added \
				automatically at build() time, if needed at all.""");
			}
			// (2) Validate characters in name
			for (int i = 0; i < name.length(); i++) {
				if (!AsciiChars.isTchar(name.charAt(i))) { // character is not Tchar
					throw new IllegalArgumentException("Invalid character in header field name.");
				}
			}
			// (3) Validate characters in value
			for (int i = 0; i < value.length(); i++) {
				int c = value.charAt(i);
				if (c > 0x7E || (c < 0x20 && c != 0x09)) { // c is not VCHAR (visible, printing character)
					// nor SP (0x20) nor HTAB (0x09)
					throw new IllegalArgumentException("Invalid character in header field value.");
				}
			}
			headers.add(Map.entry(name, value));
			return this;
		}
		
		public Builder body(BodyPublisher bodyPub) {
			this.bodyPub = bodyPub;
			return this;
		}
		
		public Builder useGzipTransferEncoding(boolean use) {
			gzipTransferEncoding = use;
			return this;
		}
		
		public Builder closeConnectionAtEnd(boolean close) {
			closeConnection = close;
			return this;
		}
		
		public HttpResponse build() throws IOException { // throws if file size cannot be read
			// Add appropriate "Content-Length" and "Transfer-Encoding" headers, if body is present
			TransferEncoding transferEncoding;
			if (bodyPub instanceof NoBody infBody) {
				if (infBody.infHeader() != null) {
					headers.add(infBody.infHeader());
				}
				if (closeConnection) {
					transferEncoding = TransferEncoding.NONE_AND_CLOSE;
				} else {
					transferEncoding = TransferEncoding.NONE;
				}
			} else {
				if (gzipTransferEncoding) {
					if (closeConnection) {
						headers.add(Map.entry("Transfer-Encoding", "gzip"));
						transferEncoding = TransferEncoding.GZIP_AND_CLOSE;
					} else {
						headers.add(Map.entry("Transfer-Encoding", "gzip, chunked"));
						transferEncoding = TransferEncoding.GZIP_AND_CHUNKED;
					}
				} else {
					long contentLength = bodyPub.contentLength();
					if (contentLength < 0) { // unknown length
						if (closeConnection) {
							transferEncoding = TransferEncoding.NONE_AND_CLOSE;
						} else {
							headers.add(Map.entry("Transfer-Encoding", "chunked"));
							transferEncoding = TransferEncoding.CHUNKED;
						}
					} else {
						headers.add(Map.entry("Content-Length", Long.toString(contentLength)));
						if (closeConnection) {
							transferEncoding = TransferEncoding.NONE_AND_CLOSE;
						} else {
							transferEncoding = TransferEncoding.NONE;
						}
					}
				}
			}
			// construct response
			return new HttpResponse(version, status, headers, bodyPub, transferEncoding);
		}
		
	}
	
	private enum TransferEncoding {
		NONE, // use no transfer encoding, keep connection alive
		NONE_AND_CLOSE, // no transfer encoding, close connection
		CHUNKED, // use chunked transfer encoding and keep conection alive
		GZIP_AND_CHUNKED, // use gzip and then chunked transfer encoding, keep connection alive
		GZIP_AND_CLOSE // use gzip transfer encoding and close connection
	}
	
	public static sealed interface BodyPublisher
		permits BodyPublisherOfByteArray,
				BodyPublisherOfFile,
				BodyPublisherOfInputStream,
				NoBody
	{
			void writeTo(OutputStream out) throws IOException;
			
			long contentLength() throws IOException; /* If negative, then the length is not known
			* and "Transfer-Encoding: chunked" will be used to write the body content to the
			* output stream.
			* IOException is thrown if the source is a file and file size cannot be obtained. */
			
			default void writeChunkedTo(OutputStream out) throws IOException {
				ChunkedOutputStream chunkedOut = new ChunkedOutputStream(out, 1024);
				writeTo(chunkedOut);
				chunkedOut.finish(); // writes everything and flushes, but does not close out
			}
			
			default void writeGzippedTo(ChunkedOutputStream chunkedOut) throws IOException {
				GZIPOutputStream gzipOut = new GZIPOutputStream(chunkedOut);
				writeTo(gzipOut);
				gzipOut.flush();
				gzipOut.finish();
				chunkedOut.finish(); // writes last-chunk and flushes, but does not close underlying output stream
			}
			
			default void writeGzippedTo(OutputStream out) throws IOException {
				GZIPOutputStream gzipOut = new GZIPOutputStream(out);
				writeTo(gzipOut);
				gzipOut.close(); // this will also close the connection
			}
			
			public static BodyPublisher ofByteArray(byte[] b) {
				return new BodyPublisherOfByteArray(b, 0, b.length);
			}
			
			public static BodyPublisher ofByteArray(byte[] b, int offset, int length) {
				if (offset < 0 || length < 0 || offset + length > b.length) {
					throw new IllegalArgumentException();
				}
				return new BodyPublisherOfByteArray(b, offset, length);
			}
			
			public static BodyPublisher ofFile(Path path) throws FileNotFoundException {
				if (!Files.isRegularFile(path)) {
					throw new FileNotFoundException();
				}
				return new BodyPublisherOfFile(path);
			}
			
			public static BodyPublisher ofString(String s) {
				return BodyPublisher.ofByteArray(s.getBytes(UTF_8));
			}
			
			public static BodyPublisher ofString(String s, Charset charset) {
				return BodyPublisher.ofByteArray(s.getBytes(charset));
			}
			
			public static BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
				return new BodyPublisherOfInputStream(streamSupplier);
			}
			
			public static BodyPublisher noBody() {
				return NO_BODY;
			}
			
			public static BodyPublisher noBodyInformationalContentLength(long informationalContentLength) {
				if (informationalContentLength < 0) {
					throw new IllegalArgumentException();
				}
				return new NoBody(Map.entry("Content-Length", Long.toString(informationalContentLength)));
			}
			
			public static BodyPublisher noBodyInformationalTransferEncoding(boolean informationalGzip, boolean informationalChunked) {
				if (informationalGzip && informationalChunked) {
					return NO_BODY_INFORMATIONAL_GZIP_AND_CHUNKED;
				} else if (informationalChunked) {
					return NO_BODY_INFORMATIONAL_CHUNKED;
				} else if (informationalGzip) {
					return NO_BODY_INFORMATIONAL_GZIP;
				} else {
					return NO_BODY;
				}
			}
			
			/* More BodyPublishers might be implemented in the future.
			 * For inspiration, visit
			 * https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpRequest.BodyPublishers.html
			 */
	}
	
	private static record BodyPublisherOfByteArray(byte[] b, int offset, int length) implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			out.write(b, offset, length);
		}
		public long contentLength() {
			return length;
		}
	}
	
	private static record BodyPublisherOfFile(Path path) implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			try (var fileStream = new BufferedInputStream(Files.newInputStream(path))) {
				fileStream.transferTo(out);
			}
		}
		public long contentLength() throws IOException {
			return Files.size(path);
		}
	}
	
	private static record BodyPublisherOfInputStream(Supplier<? extends InputStream> streamSupplier)
		implements BodyPublisher {
		public void writeTo(OutputStream out) throws IOException {
			var inputStream = streamSupplier.get();
			inputStream.transferTo(out);
		}
		public long contentLength() {
			return -7L; // unknown length at this time; writing to out will be chunked
		}
	}
	
	private static record NoBody(Map.Entry<String, String> infHeader) implements BodyPublisher {
		public void writeTo(OutputStream out) {} // Do nothing.
		public long contentLength() {
			return 0L;
		}
	}
	
	private static final NoBody NO_BODY = new NoBody(null);
	private static final NoBody NO_BODY_INFORMATIONAL_GZIP_AND_CHUNKED = new NoBody(Map.entry("Transfer-Encoding", "gzip, chunked"));
	private static final NoBody NO_BODY_INFORMATIONAL_CHUNKED = new NoBody(Map.entry("Transfer-Encoding", "chunked"));
	private static final NoBody NO_BODY_INFORMATIONAL_GZIP = new NoBody(Map.entry("Transfer-Encoding", "gzip"));

}