package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AlphaNumericStringComparatorTest {
	@Test
	public void sortLetters() {
		List<String> input = Arrays.asList("b", "c", "a", "ad", "a");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("a", "a", "ad", "b", "c");
	}

	@Test
	public void sortIntegers() {
		List<String> input = Arrays.asList("2", "20", "10", "1");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("1", "2", "10", "20");
	}

	@Test
	public void sortDoubles() {
		List<String> input = Arrays.asList("1.0", "1.3", "1.2", "1.1");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("1.0", "1.1", "1.2", "1.3");
	}

	@Test
	public void sortWithNumberSuffix() {
		List<String> input = Arrays.asList("foo2", "foo20", "foo10", "foo1");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("foo1", "foo2", "foo10", "foo20");
	}

	@Test
	public void sortEmpty() {
		List<String> input = Arrays.asList("", "", "", "");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("", "", "", "");
	}

	@Test
	public void sortEquals() {
		List<String> input = Arrays.asList("a", "a", "a", "a");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("a", "a", "a", "a");
	}

	@Test
	public void sortDifferentLengths() {
		List<String> input = Arrays.asList("", "a", "a1a", "a1", "a1aaa", "a1aa");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("", "a", "a1", "a1a", "a1aa", "a1aaa");
	}

	@Test
	public void sortDates() {
		List<String> input = Arrays.asList("20180604", "20180603", "20180602", "20180601");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("20180601", "20180602", "20180603", "20180604");
	}

	@Test
	public void sortMisc() {
		List<String> input = Arrays.asList("a.1", "1.a", "2.a", "b.1");
		input.sort(new AlphaNumericStringComparator());
		assertThat(input).containsExactly("1.a", "2.a", "a.1", "b.1");
	}
}