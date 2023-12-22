/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
package hosh.modules.filesystem;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.*;
import hosh.spi.Module;
import hosh.spi.Record;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class FormattingModule implements Module {

    @Override
    public void initialize(CommandRegistry registry) {
        registry.registerCommand("csv", Csv::new);
    }

    @Description("read or write CSV files")
    @Examples({
            @Example(command = "csv read file.csv | take 10", description = "take 10 records out of file.csv"),
            @Example(command = "ls | csv write file.csv ", description = "write output of a command as csv"),
    })
    public static class Csv implements Command, StateAware {

        private State state;

        @Override
        public void setState(State state) {
            this.state = state;
        }

        @Override
        public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
            if (args.size() != 2) {
                err.send(Errors.usage("csv write|read file"));
                return ExitStatus.error();
            }
            final Path path = state.getCwd().resolve(args.get(1));
            if (args.get(0).equals("write")) {
                try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    CSVPrinter csvPrinter = null;
                    Iterable<Record> records = InputChannel.iterate(in);
                    for (Record record : records) {
                        if (csvPrinter == null) {
                            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setDelimiter(',').setHeader(record.keys().map(Key::name).toArray(String[]::new)).build();
                            csvPrinter = csvFormat.print(writer);
                        }
                        Iterator<Value> iterator = record.values().iterator();
                        while (iterator.hasNext()) {
                            Value value = iterator.next();
                            String unwrap = value.unwrap(String.class).orElseThrow();
                            csvPrinter.print(unwrap);
                        }
                        csvPrinter.println();
                    }
                    return ExitStatus.success();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (args.get(0).equals("read")) {
                return ExitStatus.error();
            } else {
                err.send(Errors.usage("csv write|read file"));
                return ExitStatus.error();
            }
        }
    }

}
