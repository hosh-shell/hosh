package org.hosh.runtime;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 */
public class AlphaNumericStringComparator implements Comparator<String> {
	private static final Pattern CHUNK = Pattern.compile("(\\d+)|(\\D+)");

	@Override
	public int compare(String s1, String s2) {
		int compareValue = 0;
		Matcher s1ChunkMatcher = CHUNK.matcher(s1);
		Matcher s2ChunkMatcher = CHUNK.matcher(s2);
		while (s1ChunkMatcher.find() && s2ChunkMatcher.find() && compareValue == 0) {
			String s1ChunkValue = s1ChunkMatcher.group();
			String s2ChunkValue = s2ChunkMatcher.group();
			try {
				Integer s1Integer = Integer.valueOf(s1ChunkValue);
				Integer s2Integer = Integer.valueOf(s2ChunkValue);
				compareValue = s1Integer.compareTo(s2Integer);
			} catch (NumberFormatException e) {
				// not a number, use string comparison.
				compareValue = s1ChunkValue.compareTo(s2ChunkValue);
			}
			// if they are equal thus far, but one has more left, it should come after the
			// one that doesn't.
			if (compareValue == 0) {
				if (s1ChunkMatcher.hitEnd() && !s2ChunkMatcher.hitEnd()) {
					compareValue = -1;
				} else if (!s1ChunkMatcher.hitEnd() && s2ChunkMatcher.hitEnd()) {
					compareValue = 1;
				}
			}
		}
		return compareValue;
	}
}
