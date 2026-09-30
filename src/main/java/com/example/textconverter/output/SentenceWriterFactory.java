package com.example.textconverter.output;

import com.example.textconverter.cli.OutputFormat;
import java.nio.file.Path;
import java.io.IOException;
import java.io.Writer;

public final class SentenceWriterFactory {
    private SentenceWriterFactory() { }

    public static SentenceWriter create(OutputFormat format, Writer output, Path temporaryDirectory)
            throws IOException {
        return switch (format) {
            case XML -> new XmlSentenceWriter(output);
            case CSV -> new CsvSentenceWriter(output, temporaryDirectory);
        };
    }
}
