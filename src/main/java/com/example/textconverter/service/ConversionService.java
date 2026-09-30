package com.example.textconverter.service;

import com.example.textconverter.cli.OutputFormat;
import com.example.textconverter.error.InvalidInputException;
import com.example.textconverter.io.CheckedOutputStream;
import com.example.textconverter.model.SentenceBatch;
import com.example.textconverter.output.*;
import com.example.textconverter.parser.SentenceBatchReader;
import com.example.textconverter.sorting.InMemoryWordSorter;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.Path;

/** Converts input in batches of complete sentences. */
public final class ConversionService {
    public void convert(InputStream input, OutputStream output, OutputFormat format, Path temp)
            throws IOException {

        Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));
        Writer target = new BufferedWriter(new OutputStreamWriter(
                new CheckedOutputStream(output), StandardCharsets.UTF_8));

        try (SentenceWriter writer = SentenceWriterFactory.create(format, target, temp)) {
            writer.start();
            var parser = new SentenceBatchReader(reader);
            var sorter = new InMemoryWordSorter();
            while (true) {
                SentenceBatch batch;
                try {
                    batch = parser.readBatch();
                } catch (CharacterCodingException exception) {
                    throw new InvalidInputException("Input is not valid UTF-8", exception);
                }
                if (batch == null) break;
                sorter.sort(batch);
                writer.writeBatch(batch);
            }
            writer.finish();

            target.flush();
        }
    }
}
