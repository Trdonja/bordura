package http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class ChunkedOutputStream extends FilterOutputStream {

	private byte[] buf; // buffer for a chunk
	private int count; // number of valid bytes in the buffer
		
	ChunkedOutputStream(OutputStream out, int size) {
		super(out);
		if (size < 1) {
			throw new IllegalArgumentException("Size of ChunkedOutputStream buffer must be positive");
		}
		buf = new byte[size];
		count = 0;
	}
	
	ChunkedOutputStream(OutputStream out) {
		super(out);
		buf = new byte[1024];
		count = 0;
	}
	
	@Override
	public void write(int b) throws IOException {
		if (count == buf.length) {
			writeAsChunk(); // this also sets count to 0
		}
		buf[count] = (byte) b;
		count++;
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		int available = buf.length - count;
		while (len > available) {
			System.arraycopy(b, off, buf, count, available);
			count = buf.length; // writeAsChunk() will use this value
			writeAsChunk(); // this also puts count to 0
			off = off + available;
			len = len - available;
			available = buf.length;
		}
		System.arraycopy(b, off, buf, count, len);
		count = count + len;
	}
	
	/* Writes any present bytes in buffer as a chunk to the underlying output stream
	 * and resets the buffer.
	*/
	private void writeAsChunk() throws IOException {
		if (count == 0) {
			return;
		}
		out.write(Integer.toString(count, 16).getBytes(StandardCharsets.US_ASCII));
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		out.write(buf, 0, count);
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		count = 0;
	}
	
	/* Writes any present bytes in buffer as a chunk to the underlying output stream,
	 * resets the buffer and flushes the underlying output stream.
	*/
	@Override
	public void flush() throws IOException { // flushes buffer as a chunk to the underlying output stream
		writeAsChunk(); // this also puts count to 0
		out.flush();
	}
	
	@Override
	public void close() throws IOException {
		finish();
		out.close();
	}
	
	/* Writes any remaining bytes in buffer as a chunk to the underlying output stream,
	 * followed by last-chunk and the last CRLF. Finally, it flushes the underlying stream.
	 * Does NOT close the underlying output stream.
	*/
	void finish() throws IOException {
		writeAsChunk(); // this also puts count to 0
		out.write((byte) 0x30); // ASCII zero, '0'
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		out.write((byte) AsciiChars.CR);
		out.write((byte) AsciiChars.LF);
		out.flush();
		buf = null;
		count = 0;
	}
	
}
