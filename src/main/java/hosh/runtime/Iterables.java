package hosh.runtime;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Iterables {

	private Iterables() {
	}

	// adapter of method producing Optional<T> to Iterable<T>
	public static <T> Iterable<T> over(Producer<T> producer) {
		return new IterableImplementation<>(producer);
	}

	public static <T> Iterator<T> of(Producer<T> producer) {
		return new OptionalIterator<T>(producer);
	}

	private static final class IterableImplementation<T> implements Iterable<T> {

		private final Producer<T> producer;

		private IterableImplementation(Producer<T> producer) {
			this.producer = producer;
		}

		@Override
		public Iterator<T> iterator() {
			return new OptionalIterator<T>(producer);
		}
	}

	@FunctionalInterface
	public interface Producer<T> {

		Optional<T> produce();
	}

	private static class OptionalIterator<T> implements Iterator<T> {

		private final Producer<T> producer;

		private T next = null;

		public OptionalIterator(Producer<T> producer) {
			this.producer = producer;
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			Optional<T> maybeNext = producer.produce();
			if (maybeNext.isPresent()) {
				next = maybeNext.get();
				return true;
			}
			return false;
		}

		@Override
		public T next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			T result = next;
			next = null;
			return result;
		}
	}
}
