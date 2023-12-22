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
package hosh.modules.formatting;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.*;
import hosh.spi.Module;
import hosh.spi.Record;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class FormattingModule implements Module {

    @Override
    public void initialize(CommandRegistry registry) {
        registry.registerCommand("csv", Csv::new);
    }

    @Description("read or write CSV files")
    @Examples({
            @Example(command = "lines file.csv | csv load | take 10", description = "take 10 records out of file.csv"),
            @Example(command = "ls | csv save | ", description = "write output of a command as csv"),
    })
    public static class Csv implements Command {

        @Override
        public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
            if (args.size() != 1) {
                return usage(err);
            }
            String action = args.get(0);
            try {
                if (action.equals("load")) {
                    handleLoad(in, out);
                    return ExitStatus.success();
                } else if (action.equals("save")) {
                    handleSave(in, out);
                    return ExitStatus.error();
                } else {
                    return usage(err);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private ExitStatus usage(OutputChannel err) {
            err.send(Errors.usage("csv load|save"));
            return ExitStatus.error();
        }

        private static final CsvMapper CSV_MAPPER = new CsvMapper();

        // convert incoming records into a single "text" record
        private void handleSave(InputChannel in, OutputChannel out) throws IOException {
            String header = null;
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter, true);
            SequenceWriter sequenceWriter = CSV_MAPPER.writer().writeValues(stringWriter);

            for (Record record : InputChannel.iterate(in)) {
                if (header == null) {
                    header = record.keys().map(Key::name).collect(Collectors.joining(","));
                    out.send(Records.singleton(Keys.TEXT, Values.ofText(header)));
                }
                Iterator<Value> values = record.values().iterator();
                while (values.hasNext()) {
                    Value value = values.next();
                    value.print(printWriter, Locale.getDefault());

                }
                stringWriter.getBuffer().setLength(stringWriter.getBuffer().length()-1); // drop last ,
                String text = stringWriter.toString();
                out.send(Records.singleton(Keys.TEXT, Values.ofText(text)));
                stringWriter.getBuffer().setLength(0);
            }
        }

        // converting incoming text records into record
        // what about types?
        private void handleLoad(InputChannel in, OutputChannel out) throws IOException {
            for (Record record : InputChannel.iterate(in)) {
                Optional<Value> value = record.value(Keys.TEXT);
            }
        }
    }

}
