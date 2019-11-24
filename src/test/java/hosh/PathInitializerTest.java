package hosh;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PathInitializerTest {

	private PathInitializer sut;

	@BeforeEach
	public void setUp() {
		sut = new PathInitializer();
	}

	@Test
	public void nullPathVariable() {
		List<Path> path = sut.initializePath(null);
		assertThat(path).isEmpty();
	}

	@Test
	public void emptyEmptyVariable() {
		List<Path> path = sut.initializePath("");
		assertThat(path).isEmpty();
	}

	@Test
	public void pathVariableWithOneElement() {
		List<Path> path = sut.initializePath("/bin");
		assertThat(path).containsExactly(Path.of("/bin"));
	}

	@Test
	public void pathVariableWithTwoElements() {
		List<Path> path = sut.initializePath("/bin" + File.pathSeparator + "/sbin");
		assertThat(path).containsExactly(Path.of("/bin"), Path.of("/sbin"));
	}

	@Test
	public void pathVariableWithEmptyElement() {
		List<Path> path = sut.initializePath("/bin" + File.pathSeparator + "   " + File.pathSeparator + "/sbin");
		assertThat(path).containsExactly(Path.of("/bin"), Path.of("/sbin"));
	}
}
