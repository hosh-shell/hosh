package org.hosh;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AlphaNumericStringComparatorTest {

    @Test
    public void sortIntegers() {
        List<String> input = Arrays.asList("2", "20", "10", "1");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("1", "2", "10", "20");
        Assert.assertEquals(expected, input);
    }

      @Test
    public void sortDoubles() {
        List<String> input = Arrays.asList("1.0", "1.3", "1.2", "1.1");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("1.0", "1.1", "1.2", "1.3");
        Assert.assertEquals(expected, input);
    }

    @Test
    public void sortWithNumberSuffix() {
        List<String> input = Arrays.asList("foo2", "foo20", "foo10", "foo1");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("foo1", "foo2", "foo10", "foo20");
        Assert.assertEquals(expected, input);
    }

    @Test
    public void sortEmpty() {
        List<String> input = Arrays.asList("", "", "", "");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("", "", "", "");
        Assert.assertEquals(expected, input);
    }

    @Test
    public void sortEquals() {
        List<String> input = Arrays.asList("a", "a", "a", "a");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("a", "a", "a", "a");
        Assert.assertEquals(expected, input);
    }

    @Test
    public void sortDifferentLengths() {
        List<String> input = Arrays.asList("a1a", "a1", "a1aaa", "a1aa");

        Collections.sort(input, new AlphaNumericStringComparator());

        List<String> expected = Arrays.asList("a1", "a1a", "a1aa", "a1aaa");
        Assert.assertEquals(expected, input);
    }

}