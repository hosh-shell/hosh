/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package org.hosh.modules;

import java.io.UncheckedIOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import org.hosh.doc.Example;
import org.hosh.doc.Examples;
import org.hosh.doc.Help;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.Records;
import org.hosh.spi.Values;

public class NetworkModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("network", Network.class);
	}

	@Help(description = "list network interfaces")
	@Examples({
			@Example(command = "network", description = "list all network interfaces")
	})
	public static class Network implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
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
							.entry(Keys.of("hwaddress"), Values.ofText(formatHex(ni.getHardwareAddress())))
							.entry(Keys.of("address"), Values.ofText(formatInterfaceAddress(ni)))
							.build();
					out.send(record);
				}
				return ExitStatus.success();
			} catch (SocketException e) {
				throw new UncheckedIOException(e);
			}
		}

		private String formatInterfaceAddress(NetworkInterface ni) {
			var interfaceAddresses = ni.getInterfaceAddresses();
			if (interfaceAddresses.isEmpty()) {
				return "";
			}
			return interfaceAddresses.get(0).getAddress().getHostAddress();
		}

		private String formatHex(byte[] bytes) {
			try (Formatter formatter = new Formatter()) {
				if (bytes != null) {
					for (byte b : bytes) {
						formatter.format("%02x", b);
					}
				}
				return formatter.toString();
			}
		}
	}
}
