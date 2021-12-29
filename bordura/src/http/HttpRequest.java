/**
 * 
 */
package http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Domen
 *
 */
public class HttpRequest {
	
	public static enum Method {GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE}
	
	private final Method method;
	private final String target;
	private final HttpVersion version;
	private final Map<String, List<String>> headers;
	private final Optional<byte[]> body;
	
	private static final int CR = 0x0D; // carriage return, '\r'
	private static final int LF = 0x0A; // line feed, '\n'
	private static final int SP = 0x20; // space, ' '
	private static final int HTAB = 0x09; // horizontal tab, '\t'
	private static final int COL = 0x3A; // colon, ':'
	private static final Charset ASCII = StandardCharsets.US_ASCII;
	
	private HttpRequest (Method method, String resourcePath, HttpVersion version, Map<String, List<String>> headers, byte[] body) {
		this.method = method;
		this.target = resourcePath;
		this.version = version;
		this.headers = headers;
		this.body = Optional.ofNullable(body);
	}
	
	public Method method() {
		return this.method;
	}
	
	public String target() {
		return this.target;
	}
	
	public HttpVersion version() {
		return this.version;
	}
	
	public Map<String, List<String>> headers() {
		return this.headers;
	}
	
	public Optional<byte[]> body() {
		return this.body;
	}
	
	public static HttpRequest readFrom(InputStream in) throws IOException, HttpException {
		// Read request line
		final ByteBuffer buffer = new ByteBuffer();
		final Method method = readMethod(in, buffer);
		buffer.reset();
		final String target = readTarget(in, buffer);
		buffer.reset();
		final HttpVersion version = readVersion(in, buffer);
		buffer.reset();
		// Read header fields
		Map<String, List<String>> headers = new HashMap<>();
		HeaderReadResult result = readHeaderField(in, buffer);
		int totalHeaderSize = 0;
		while (result instanceof HeaderField field) {
			totalHeaderSize = totalHeaderSize + field.size;
			if (totalHeaderSize > 196608) { // > 24 * 8 kB, more than 24 full-sized header fields
				throw new HttpException(431);
			}
			if (headers.containsKey(field.name)) {
				headers.get(field.name).add(field.value);
			} else {
				LinkedList<String> list = new LinkedList<>();
				list.add(field.value);
				headers.put(field.name, list);
			}
			buffer.reset();
			result = readHeaderField(in, buffer);
		}
		for (var field : headers.entrySet()) {
			headers.put(field.getKey(), List.copyOf(field.getValue())); // make each list unmodifiable
		}
		headers = Map.copyOf(headers); // make map unmodifiable
		// TODO: Read body
		final byte[] body = null;
		return new HttpRequest(method, target, version, headers, body);
	}
	
	private static Method readMethod(InputStream in, ByteBuffer buffer) throws IOException, HttpException {
		for (int c = in.read(), j = 1; c != SP; c = in.read(), j++) {
			if (c < 0x41 || c > 0x5A || j > 7) { // if c is not upper-case letter or if word is too long
				throw new HttpException(400);
			}
			buffer.put((byte) c);
		}
		String methodString = buffer.read(ASCII);
		try {
			Method method = Method.valueOf(methodString);
			return method;
		} catch (IllegalArgumentException e) {
			throw new HttpException(400);
		}
	}
	
	private static String readTarget(InputStream in, ByteBuffer buffer) throws IOException, HttpException {
		int c = in.read();
		if (c != 0x2F) { // first character should always be '/', 0x2F
			throw new HttpException(400);
		}
		buffer.put((byte) c);
		c = in.read();
		int j = 2; // number of bytes read
		final int lengthLimit = 8192; // maximum number of bytes to be read
		while (c != SP) {
			if (j > lengthLimit) {
				throw new HttpException(414); // URI too long
			}
			if ((c >= 0x61 && c <= 0x7A) // lower-case letter
				|| (c >= 0x40 && c <= 0x5A) // '@' or upper-case letter
				|| (c >= 0x26 && c <= 0x3B) // '&', '\'', '(', ')', '*', '+', ',', '-', '.', digit, ':' or ';'
				|| (c == 0x2F) // '/'
				|| (c == 0x5F) // '_'
				|| (c == 0x3F) // '?'
				|| (c == 0x3D) // '='
				|| (c == 0x21) // '!'
				|| (c == 0x7E) // '~'
				|| (c == 0x24))// '$'
			{
				buffer.put((byte) c); // it is a valid character
			} else if (c == 0x25) { // '%', percent-escaped character; "%" HEXDIG HEXDIG
				buffer.put((byte) c); // put '%'
				c = in.read();
				if (!isHexDigit(c)) {
					throw new HttpException(400);
				}
				buffer.put((byte) c); // put first hexdigit
				c = in.read(); // read second hexdigit
				if (!isHexDigit(c)) {
					throw new HttpException(400);
				}
				buffer.put((byte) c); // put second hexdigit
				j = j + 2; // two hexdigits were read
			} else {
				throw new HttpException(400);
			}
			c = in.read();
			j++;
		}
		return buffer.read(ASCII);
		// this string can be further converted toCharArray() an then split into pieces:
		// (1) String[] segments <- everything before first occurance of '?', delimited into chunks by '/'
		// (2) String query <- everything after first occurance of '?'
		// both (1) and (2) should decode percent-escaped sequences after parsing
	}
	
	private static HttpVersion readVersion(InputStream in, ByteBuffer buffer) throws IOException, HttpException {
		if (in.read() != 0x48 || in.read() != 0x54
			|| in.read() != 0x54 || in.read() != 0x50 || in.read() != 0x2F) { // if not "HTTP/"
			throw new HttpException(400);
		}
		int majorVersion = -1;
		int minorVersion = 0;
		int c = in.read();
		if (c >= 0x30 && c <= 0x39) { // c is a digit
			majorVersion = c - 0x30;
		} else {
			throw new HttpException(400);
		}
		if (in.read() == 0x2E) { // c is '.'
			c = in.read();
			if (c >= 0x30 && c <= 0x39) { // c is a digit
				minorVersion = c - 0x30;
			} else {
				throw new HttpException(400);
			}
		}
		if (in.read() != CR || in.read() != LF) {
			throw new HttpException(400);
		}
		try {
			return HttpVersion.get(majorVersion, minorVersion);
		} catch (IllegalArgumentException e) {
			throw new HttpException(400, e.getMessage());
		}
	}
	
	private static HeaderReadResult readHeaderField(InputStream in, ByteBuffer buffer)  throws IOException, HttpException {
		int c = in.read();
		if (c == CR) {
			if (in.read() == LF) {
				return HeaderReadResult.ofCRLF(); // Signals that it is a CRLF pair
			} else {
				throw new HttpException(400);
			}
		}
		int j = 1;
		final int limit = 8192;
		// Read header field name (until colon)
		while (c != COL) {
			if (!isTchar(c)) {
				throw new HttpException(400);
			} else if (j > limit) {
				throw new HttpException(431);
			} else {
				// normalize names to lower-case
				if (c >= 0x41 && c <= 0x5A) { // if c is upper-case letter, A-Z
					c = c + 0x20; // convert it to lower-case counterpart, a-z
				}
				buffer.put((byte) c);
				c = in.read();
				j++;
			}
		}
		String name = buffer.read(ASCII);
		buffer.reset();
		// Read header field value (until CRLF)
		c = in.read();
		j++;
		while (c == SP || c == HTAB) { // skip any leading whitespaces
			if (j > limit) {
				throw new HttpException(431);
			} else {
				c = in.read();
				j++;
			}
		}
		while (c != CR) {
			if (j > limit) {
				throw new HttpException(431);
			} else if ((c >= 0x20 && c <= 0x7E) || (c == HTAB)) {
				// c is VCHAR (visible, printing character) or SP (0x20) or HTAB (0x09)
				buffer.put((byte) c);
				c = in.read();
				j++;
			} else {
				throw new HttpException(400);
			}
		}
		while (!buffer.empty()) { // remove trailing whitespaces
			int p = buffer.peek(); // inspect last element in buffer
			if (p == SP || p == HTAB) {
				buffer.pop(); // remove it
			} else {
				break;
			}
		}
		if (c == LF) {
			String value = buffer.read(ASCII);
			return HeaderReadResult.ofPair(name, value); // Signals that header was inserted
		} else {
			throw new HttpException(400);
		}
	}
	
	static boolean isTchar(int c) {
		return     (c >= 0x5E && c < 0x7A) // '^', '_', '`' or lower-case letter
				|| (c >= 0x41 && c <= 0x5A) // upper-case letter
				|| (c >= 0x30 && c <= 0x39) // digit
				|| (c >= 0x23 && c <= 0x27) // '#', '$', '%', '&' or '\''
				|| (c == 0x21) || (c == 0x2A) || (c == 0x2B) || (c == 0x2D) || (c == 0x2E) // '!', '*', '+', '-' or '.'
				|| (c == 0x7C) || (c == 0x7E); // '|' or '~'
	}
	
	private static boolean isHexDigit(int c) {
		return (c >= 0x30 && c <= 0x39)  // digit 0-9
			|| (c >= 0x41 && c <= 0x46)  // letter A-F
			|| (c >= 0x61 && c <= 0x66); // letter a-f
	}
	
	private static sealed interface HeaderReadResult permits HeaderField, CrLf {
		
		private static HeaderReadResult ofPair(String name, String value) {
			return new HeaderField(name, value, name.length() + value.length());
		}
		
		private static HeaderReadResult ofCRLF() {
			return CRLF;
		}
		
		static final CrLf CRLF = new CrLf();
		
	}
	
	private static record HeaderField(String name, String value, int size) implements HeaderReadResult {}
	
	private static record CrLf() implements HeaderReadResult {}
	
}
