package org.hosh.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/** Built-in values. NB: concrete types are not exposed by purpose */
public class Values {

	private Values() {
	}

	public static Value ofText(@Nonnull String text) {
		return new Text(text);
	}

	public static Value ofSize(@Nonnegative long value, @Nonnull Unit unit) {
		return new Size(value, unit);
	}

	public static Value ofPath(@Nonnull Path path) {
		return new LocalPath(path);
	}

	public enum Unit {
		B, KB, MB, GB, TB
	}

	/**
	 * Generic text, make sure that text hasn't any number or date formatted without
	 * the current locale.
	 */
	private static class Text implements Value {

		private final String value;

		public Text(@Nonnull String value) {
			this.value = value;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(value);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Text[%s]", value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Text) {
				Text that = (Text) obj;
				return Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

	}

	/**
	 * Used to represent a size of a file, etc.
	 */
	private static class Size implements Value {

		private final long value;
		private final Unit unit;

		public Size(@Nonnegative long value, @Nonnull Unit unit) {
			this.value = value;
			this.unit = unit;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			String formattedValue = String.format(locale, "%d%s", value, unit);
			try {
				appendable.append(formattedValue);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Size[%s%s]", value, unit);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Size) {
				Size that = (Size) obj;
				return Objects.equals(this.value, that.value) && Objects.equals(this.unit, that.unit);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, unit);
		}

	}

	private static class LocalPath implements Value {

		private final Path path;

		public LocalPath(@Nonnull Path path) {
			this.path = path;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(path.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("LocalPath[%s]", path);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LocalPath) {
				LocalPath that = (LocalPath) obj;
				return Objects.equals(this.path, that.path);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}

	}
}
