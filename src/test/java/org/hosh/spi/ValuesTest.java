package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Locale;

import org.hosh.spi.Values.Unit;
import org.hosh.spi.ValuesTest.LocalPathValueTest;
import org.hosh.spi.ValuesTest.SizeValueTest;
import org.hosh.spi.ValuesTest.TextValueTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import nl.jqno.equalsverifier.EqualsVerifier;

@RunWith(Suite.class)
@SuiteClasses({
		TextValueTest.class,
		SizeValueTest.class,
		LocalPathValueTest.class
})
public class ValuesTest {

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class TextValueTest {

		@Mock
		private Appendable appendable;

		@Test
		public void appendOk() throws IOException {
			Values.ofText("aaa").append(appendable, Locale.getDefault());
			then(appendable).should().append("aaa");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(Mockito.any())).willThrow(IOException.class);
			Values.ofText("aaa").append(appendable, Locale.getDefault());
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.Text.class).verify();
		}

	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SizeValueTest {

		@Mock
		private Appendable appendable;

		@Test
		public void appendWithUkLocale() throws IOException {
			Values.ofSize(10_000_000, Unit.B).append(appendable, Locale.UK);
			then(appendable).should().append("10,000,000B");
		}

		@Test
		public void appendWithItalianLocale() throws IOException {
			Values.ofSize(10_000_000, Unit.B).append(appendable, Locale.ITALIAN);
			then(appendable).should().append("10.000.000B");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(Mockito.any())).willThrow(IOException.class);
			Values.ofSize(10, Unit.GB).append(appendable, Locale.getDefault());
		}

		@Test
		public void humanizedSize() {
			assertThat(Values.ofHumanizedSize(0L)).hasToString("Size[0B]");
			assertThat(Values.ofHumanizedSize(512L)).hasToString("Size[512B]");
			assertThat(Values.ofHumanizedSize(1023L)).hasToString("Size[1023B]");
			assertThat(Values.ofHumanizedSize(1024L)).hasToString("Size[1KB]");
			assertThat(Values.ofHumanizedSize(1024L * 1024)).hasToString("Size[1MB]");
			assertThat(Values.ofHumanizedSize(1024L * 1024 * 2 - 1)).hasToString("Size[1MB]"); // TODO: should be 1.99MB
			assertThat(Values.ofHumanizedSize(1024L * 1024 * 1024)).hasToString("Size[1GB]");

			assertThatThrownBy(() -> Values.ofHumanizedSize(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("negative size");
		}

		@Test
		public void size() {
			assertThatThrownBy(() -> Values.ofSize(-1, Unit.B))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("negative size");

		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.Size.class).verify();
		}

	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class LocalPathValueTest {

		@Mock
		private Appendable appendable;

		@Test
		public void appendOk() throws IOException {
			Values.ofLocalPath(Paths.get(".")).append(appendable, Locale.getDefault());
			then(appendable).should().append(".");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(Mockito.any())).willThrow(IOException.class);
			Values.ofLocalPath(Paths.get(".")).append(appendable, Locale.getDefault());
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.LocalPath.class).verify();
		}

	}

}
