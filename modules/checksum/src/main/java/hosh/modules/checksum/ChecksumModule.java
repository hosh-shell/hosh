/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.modules.checksum;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.Command;
import hosh.spi.CommandArguments;
import hosh.spi.CommandName;
import hosh.spi.CommandRegistry;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Value;
import hosh.spi.Values;
import hosh.spi.Module;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ChecksumModule implements Module {

	private static final Set<String> SUPPORTED_ALGORITHMS = Set.of("MD5", "SHA-1", "SHA-256", "SHA-512");

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("to-checksum"), ToChecksum::new);
		registry.registerCommand(CommandName.constant("from-checksum"), FromChecksum::new);
	}

	@Description("compute checksum of files (MD5, SHA-1, SHA-256, SHA-512)")
	@Examples({
			@Example(description = "compute SHA-256 of a file", command = "to-checksum sha256 file.txt"),
			@Example(description = "compute SHA-256 of all files via pipeline", command = "ls | to-checksum sha256"),
			@Example(description = "compute MD5 of a file", command = "to-checksum md5 file.txt"),
	})
	public static class ToChecksum implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.isEmpty() || args.size() > 2) {
				err.send(Errors.usage("to-checksum MD5|SHA-1|SHA-256|SHA-512 file"));
				return ExitStatus.error();
			}
			String algorithm = args.get(0).asString();
			if (!SUPPORTED_ALGORITHMS.contains(algorithm)) {
				err.send(Errors.message("unsupported algorithm: %s (supported: %s)", algorithm, SUPPORTED_ALGORITHMS));
				return ExitStatus.error();
			}

			if (args.size() == 1) {
				return hashFromPipeline(algorithm, in, out, err);
			} else {
				String file = args.get(1).asString();
				return hashFiles(algorithm, file, out, err);
			}
		}

		private ExitStatus hashFromPipeline(String algorithm, InputChannel in, OutputChannel out, OutputChannel err) {
			for (hosh.spi.Record record : InputChannel.iterate(in)) {
				Optional<Value> pathValue = record.value(Keys.PATH);
				if (pathValue.isEmpty()) {
					err.send(Errors.message("record has no path key, skipping"));
					continue;
				}
				Path file = state.getCwd().resolve(pathValue.get().unwrap(Path.class).orElseThrow());
				if (!Files.isRegularFile(file)) {
					continue;
				}
				ExitStatus status = hashOneFile(algorithm, file, out, err);
				if (status.isError()) {
					return status;
				}
			}
			return ExitStatus.success();
		}

		private ExitStatus hashFiles(String algorithm, String filename, OutputChannel out, OutputChannel err) {
			Path file = state.getCwd().resolve(filename);
			if (!Files.exists(file)) {
				err.send(Errors.message("file not found: %s", file));
				return ExitStatus.error();
			}
			if (!Files.isRegularFile(file)) {
				err.send(Errors.message("not a regular file: %s", file));
				return ExitStatus.error();
			}
			ExitStatus status = hashOneFile(algorithm, file, out, err);
			if (status.isError()) {
				return status;
			}
			return ExitStatus.success();
		}

		private ExitStatus hashOneFile(String algorithm, Path file, OutputChannel out, OutputChannel err) {
			try {
				String hash = computeHash(algorithm, file);
				out.send(Records.builder()
						.entry(Keys.PATH, Values.ofPath(file))
						.entry(Keys.of("algorithm"), Values.ofText(algorithm))
						.entry(Keys.of("hash"), Values.ofText(hash))
						.build());
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (NoSuchAlgorithmException e) {
				err.send(Errors.message("unsupported algorithm: %s", algorithm));
				return ExitStatus.error();
			}
		}
	}

	@Description("parse a checksum file (sha256sum/md5sum output format) into records")
	@Examples({
			@Example(description = "read checksums from a SHA-256 checksum file", command = "from-checksum checksums.sha256"),
			@Example(description = "read and verify checksums", command = "from-checksum checksums.sha256 | to-checksum sha256"),
	})
	public static class FromChecksum implements Command, StateAware {

		// Maps hex digest length to canonical algorithm name
		private static final Map<Integer, String> LENGTH_TO_ALGORITHM = Map.of(
				32, "MD5",
				40, "SHA-1",
				64, "SHA-256",
				128, "SHA-512"
		);

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-checksum file"));
				return ExitStatus.error();
			}
			Path source = args.get(0).asPath(state);
			if (!Files.exists(source)) {
				err.send(Errors.message("file not found: %s", source));
				return ExitStatus.error();
			}
			if (!Files.isRegularFile(source)) {
				err.send(Errors.message("not a regular file: %s", source));
				return ExitStatus.error();
			}
			try {
				List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
				for (String line : lines) {
					if (line.isBlank()) {
						continue;
					}
					// standard format: "<hash>  <filename>" or "<hash> *<filename>" (binary mode)
					int sep = line.indexOf("  ");
					String hash;
					String filename;
					if (sep > 0) {
						hash = line.substring(0, sep).strip();
						filename = line.substring(sep + 2).strip();
					} else {
						int binarySep = line.indexOf(" *");
						if (binarySep <= 0) {
							err.send(Errors.message("unrecognized checksum line: %s", line));
							return ExitStatus.error();
						}
						hash = line.substring(0, binarySep).strip();
						filename = line.substring(binarySep + 2).strip();
					}
					String algorithm = LENGTH_TO_ALGORITHM.get(hash.length());
					if (algorithm == null) {
						err.send(Errors.message("unrecognized hash length %d in line: %s", hash.length(), line));
						return ExitStatus.error();
					}
					Path file = source.getParent().resolve(Path.of(filename));
					out.send(Records.builder()
							.entry(Keys.PATH, Values.ofPath(file))
							.entry(Keys.of("algorithm"), Values.ofText(algorithm))
							.entry(Keys.of("hash"), Values.ofText(hash))
							.build());
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	static String computeHash(String algorithm, Path file) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		try (InputStream is = Files.newInputStream(file)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = is.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}
}
