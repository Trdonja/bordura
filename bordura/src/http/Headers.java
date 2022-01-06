package http;

import java.util.LinkedList;
import java.util.List;

public class Headers {

	public static long readContentLength(String s) {
		return Long.parseUnsignedLong(s);
	}
	
	public static List<String> readCommaDelimitedList(String s) {
		List<String> list = new LinkedList<>();
		final int sLen = s.length();
		int begin = 0; // index, where list item begins
		while (begin < sLen) {
			int pend = begin; // to-be index of comma ',' character or end of s
			for (; pend < sLen && s.charAt(pend) != ','; pend++); // find index of period ',' char
			int end = pend - 1; // to-be index of last non-whitespace character before comma
			for (; end >= begin && (s.charAt(end) == ' ' || s.charAt(end) == '\t'); end--);
			if (end - begin >= 0) {
				list.add(s.substring(begin, end + 1));
			}
			for (begin = pend + 1; begin < sLen && (s.charAt(begin) == ' ' || s.charAt(begin) == '\t' || s.charAt(begin) == ','); begin++); // prepare begin to be the index of next list item
		}
		return list;
	}
	
}
