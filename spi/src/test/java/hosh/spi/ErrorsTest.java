package hosh.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorsTest {

	@Test
	void exceptionWithoutMessage() {
		Exception npe = new NullPointerException();
		Record result = Errors.message(npe);
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("(no message)")));
	}

	@Test
	void exceptionWithMessage() {
		Exception npe = new IllegalArgumentException("whatever");
		Record result = Errors.message(npe);
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("whatever")));
	}
}