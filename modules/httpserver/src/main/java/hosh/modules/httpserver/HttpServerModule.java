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
package hosh.modules.httpserver;

import com.sun.net.httpserver.SimpleFileServer;
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
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Values;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;

public final class HttpServerModule implements Module {

	private static final int MAX_PORT = 65535;

	@Override
	public void initialize(final CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("httpserver"), HttpServerCommand::new);
	}

	@Description("start an embedded HTTP file server")
	@Examples({
			@Example(command = "httpserver 8080 .",
					description = "start server on port 8080 serving current directory"),
			@Example(command = "httpserver 3000 /tmp",
					description = "start server on port 3000 serving /tmp directory")
	})
	public static final class HttpServerCommand implements Command, StateAware {

		private State state;
		private BusyWait busyWait = new DefaultBusyWait();

		@Override
		public void setState(State state) {
			this.state = state;
		}

		public void setBusyWait(BusyWait busyWait) {
			this.busyWait = busyWait;
		}

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("httpserver PORT DIRECTORY"));
				return ExitStatus.error();
			}

			final OptionalInt port = args.get(0).asInt();
			if (port.isEmpty() || port.getAsInt() < 1 || port.getAsInt() > MAX_PORT) {
				err.send(Errors.message("port must be a number between 1 and " + MAX_PORT));
				return ExitStatus.error();
			}

			final Path directory = args.get(1).asPath(state);
			if (!Files.isDirectory(directory)) {
				err.send(Errors.message("directory does not exist"));
				return ExitStatus.error();
			}

			// starts the servee
			final var server = SimpleFileServer.createFileServer(
					new InetSocketAddress(port.orElseThrow()),
					directory,
					SimpleFileServer.OutputLevel.INFO
			);

			try {
				server.start();
				final String startMsg = "HTTP server started on http://localhost:" + port.orElseThrow() + " serving " + directory;
				out.send(Records.singleton(Keys.TEXT, Values.ofText(startMsg)), EnumSet.of(OutputChannel.Option.DIRECT));
				busyWait.busyWait();
				return ExitStatus.success();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return ExitStatus.success();
			} finally {
				server.stop(0);
				out.send(Records.singleton(Keys.TEXT, Values.ofText("server stopped")), EnumSet.of(OutputChannel.Option.DIRECT));
			}
		}

	}

	// escamotage to allow unit testing
	public interface BusyWait {

		void busyWait() throws InterruptedException;
	}

	public static class DefaultBusyWait implements BusyWait {

		private static final Duration BUSY_WAIT = Duration.ofMillis(100);

		@Override
		public void busyWait() throws InterruptedException {
			while (!Thread.currentThread().isInterrupted()) {
				Thread.sleep(BUSY_WAIT);
			}
		}
	}

	public static class ExternalBusyWait implements BusyWait {

		private final CountDownLatch countDownLatch;

		public ExternalBusyWait(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void busyWait() throws InterruptedException {
			countDownLatch.await();
		}
	}
}
