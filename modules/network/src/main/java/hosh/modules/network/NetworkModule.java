/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
package hosh.modules.network;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Todo;
import hosh.spi.Command;
import hosh.spi.CommandRegistry;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Value;
import hosh.spi.Values;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.NetworkInterface;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class NetworkModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand("network", Network::new);
		registry.registerCommand("http", Http::new);
	}

	@Description("list network interfaces")
	@Examples({
		@Example(command = "network", description = "list all network interfaces")
	})
	public static class Network implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (!args.isEmpty()) {
				err.send(Errors.usage("network"));
				return ExitStatus.error();
			}
			try {
				Iterator<NetworkInterface> iterator = NetworkInterface.networkInterfaces().iterator();
				while (iterator.hasNext()) {
					NetworkInterface ni = iterator.next();
					Record record = Records
						.builder()
						.entry(Keys.of("alias"), Values.ofText(ni.getDisplayName()))
						.entry(Keys.of("up"), Values.ofText(ni.isUp() ? "yes" : "no"))
						.entry(Keys.of("loopback"), Values.ofText(ni.isLoopback() ? "yes" : "no"))
						.entry(Keys.of("virtual"), Values.ofText(ni.isVirtual() ? "yes" : "no"))
						.entry(Keys.of("mtu"), Values.ofNumeric(ni.getMTU()))
						.entry(Keys.of("hwaddress"), hwAddress(ni.getHardwareAddress()))
						.entry(Keys.of("address"), firstAddress(ni))
						.build();
					out.send(record);
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private Value hwAddress(byte[] hardwareAddress) {
			if (hardwareAddress == null) {
				return Values.none();
			} else {
				return Values.ofBytes(hardwareAddress);
			}
		}

		// by now, just use the first address formatted as hex
		// not perfect but a good start
		private Value firstAddress(NetworkInterface ni) {
			var interfaceAddresses = ni.getInterfaceAddresses();
			if (interfaceAddresses.isEmpty()) {
				return Values.none();
			}
			return Values.ofText(interfaceAddresses.get(0).getAddress().getHostAddress());
		}

	}

	@Description("http client (supports HTTP 1.1/2.0, HTTPS, system proxy)")
	@Examples({
		@Example(command = "http https://git.io/v9MjZ | take 10", description = "take first 10 lines of https://git.io/v9MjZ ")
	})
	@Todo(description = "support additional methods (e.g. POST, DELETE), set headers, gzip support, etc")
	public static class Http implements Command {

		private Requestor requestor = new DefaultRequestor();

		public void setRequestor(Requestor requestor) {
			this.requestor = requestor;
		}

		@Todo(description = "add version to user agent (e.g. hosh/0.0.29)")
		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("http URL"));
				return ExitStatus.error();
			}
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(args.get(0)))
				.setHeader("User-Agent", "Hosh")
				.GET()
				.build();
			try {
				HttpResponse<Stream<String>> response = requestor.send(request);
				try (Stream<String> body = response.body()) {
					body.forEach(line -> out.send(Records.singleton(Keys.TEXT, Values.ofText(line))));
				}
				return ExitStatus.success();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				err.send(Errors.message("interrupted"));
				return ExitStatus.error();
			} catch (IOException ioe) {
				err.send(Errors.message(ioe));
				return ExitStatus.error();
			}
		}

		interface Requestor {

			HttpResponse<Stream<String>> send(HttpRequest request) throws IOException, InterruptedException;
		}

		private static class DefaultRequestor implements Requestor {

			@Override
			public HttpResponse<Stream<String>> send(HttpRequest request) throws IOException, InterruptedException {
				return HttpClientHolder.getInstance().send(request, BodyHandlers.ofLines());
			}
		}

		private static class HttpClientHolder {

			private static final HttpClient INSTANCE = HttpClient.newBuilder()
				.version(Version.HTTP_2)
				.followRedirects(Redirect.NORMAL)
				.proxy(ProxySelector.getDefault())
				.executor(Runnable::run)
				.build();

			public static HttpClient getInstance() {
				return INSTANCE;
			}
		}
	}
}
