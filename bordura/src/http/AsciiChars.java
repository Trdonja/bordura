package http;

class AsciiChars {
	
	static final int CR = 0x0D; // carriage return, '\r'
	static final int LF = 0x0A; // line feed, '\n'
	static final int SP = 0x20; // space, ' '
	static final int HTAB = 0x09; // horizontal tab, '\t'
	static final int COL = 0x3A; // colon, ':'
	
	static boolean isTchar(int c) {
		return     (c >= 0x5E && c < 0x7A) // '^', '_', '`' or lower-case letter
				|| (c >= 0x41 && c <= 0x5A) // upper-case letter
				|| (c >= 0x30 && c <= 0x39) // digit
				|| (c >= 0x23 && c <= 0x27) // '#', '$', '%', '&' or '\''
				|| (c == 0x21) || (c == 0x2A) || (c == 0x2B) || (c == 0x2D) || (c == 0x2E) // '!', '*', '+', '-' or '.'
				|| (c == 0x7C) || (c == 0x7E); // '|' or '~'
	}
	
	static boolean isHexDigit(int c) {
		return (c >= 0x30 && c <= 0x39)  // digit 0-9
			|| (c >= 0x41 && c <= 0x46)  // letter A-F
			|| (c >= 0x61 && c <= 0x66); // letter a-f
	}

}
