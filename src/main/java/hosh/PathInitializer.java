package hosh;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PathInitializer {

	public List<Path> initializePath(String path) {
		return Optional.ofNullable(path)
				.stream()
				.map(s -> s.split(File.pathSeparator, 0))
				.flatMap(Arrays::stream)
				.filter(s -> !s.isBlank())
				.map(Paths::get)
				.collect(Collectors.toList());
	}
}
