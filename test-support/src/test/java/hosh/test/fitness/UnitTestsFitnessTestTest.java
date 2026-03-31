package hosh.test.fitness;

import com.tngtech.archunit.junit.AnalyzeClasses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Closeable;
import java.io.IOException;

// this feels so meta :-)
@AnalyzeClasses(packagesOf = UnitTestsFitnessTestTest.TestingTest.class)
class UnitTestsFitnessTestTest extends UnitTestsFitnessTest {

	@ExtendWith(MockitoExtension.class)
	public static class TestingTest {

		@Mock
		Closeable closeable;

		@Test
		void close() throws IOException {
			// Given
			Mockito.doNothing().when(closeable).close();

			// When
			closeable.close();

			// Then
			// just observe no errors
		}
	}
}