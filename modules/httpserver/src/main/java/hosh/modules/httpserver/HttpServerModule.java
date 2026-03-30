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

public final class HttpServerModule implements Module {

	private static final int MAX_PORT = 65535;
	private static final Duration BUSY_WAIT = Duration.ofMillis(100);

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

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(final CommandArguments args, final InputChannel in,
		                      final OutputChannel out, final OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("httpserver PORT DIRECTORY"));
				return ExitStatus.error();
			}

			final var portOpt = args.get(0).asInt();
			if (portOpt.isEmpty()) {
				err.send(Errors.message("port must be a number, got: %s", args.get(0).asString()));
				return ExitStatus.error();
			}

			final int port = portOpt.getAsInt();
			if (port < 1 || port > MAX_PORT) {
				err.send(Errors.message("port must be between 1 and " + MAX_PORT));
				return ExitStatus.error();
			}

			final Path directory = args.get(1).asPath(state);
			if (!Files.isDirectory(directory)) {
				err.send(Errors.message("directory does not exist: %s", directory));
				return ExitStatus.error();
			}

			// starts the servee
			final var server = SimpleFileServer.createFileServer(
					new InetSocketAddress(port),
					directory,
					SimpleFileServer.OutputLevel.INFO
			);

			try {
				server.start();
				final String startMsg = "HTTP server started on http://localhost:" + port + " serving " + directory;
				out.send(Records.singleton(Keys.TEXT, Values.ofText(startMsg)), EnumSet.of(OutputChannel.Option.DIRECT));
				busyWait();
				return ExitStatus.success();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return ExitStatus.success();
			} finally {
				server.stop(0);
				out.send(Records.singleton(Keys.TEXT, Values.ofText("server stopped")), EnumSet.of(OutputChannel.Option.DIRECT));
			}
		}

		// there is no other way to keep the server active?
		private void busyWait() throws InterruptedException {
			while (!Thread.currentThread().isInterrupted()) {
				Thread.sleep(BUSY_WAIT);
			}
		}
	}
}
