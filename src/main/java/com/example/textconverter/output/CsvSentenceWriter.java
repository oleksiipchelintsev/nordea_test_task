package com.example.textconverter.output;

import com.example.textconverter.model.Sentence;
import java.util.List;
import com.example.textconverter.storage.TemporaryCsvFile;
import java.io.*;
import java.nio.file.Path;

/** Stores rows temporarily until the CSV header width is known. */
public final class CsvSentenceWriter implements SentenceWriter {
    private final Writer output;
    private final TemporaryCsvFile temporaryFile;
    private final Writer rows;
    private long maximumWords;

    public CsvSentenceWriter(Writer output, Path temporaryDirectory) throws IOException {
        this.output = output;
        temporaryFile = new TemporaryCsvFile(temporaryDirectory);
        rows = temporaryFile.writer();
    }

    @Override public void start() { }

    @Override public void writeSentence(Sentence sentence, List<String> words) throws IOException {
        rows.write("Sentence " + sentence.ordinal());
        for (String word : words) {
            rows.write(", ");
            writeField(rows, word);
        }
        rows.write('\n');
        maximumWords = Math.max(maximumWords, words.size());
    }

    @Override public void finish() throws IOException {
        temporaryFile.finishWriting();
        for (long column = 0; column < maximumWords; column++) {
            output.write(", Word " + (column + 1));
        }
        output.write('\n');
        try (Reader input = temporaryFile.openReader()) {
            char[] buffer = new char[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    static void writeField(Writer target, String value) throws IOException {

        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                || !value.isEmpty() && (isSpace(value.codePointAt(0))
                || isSpace(value.codePointBefore(value.length())));
        if (!quote) { target.write(value); return; }
        target.write('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (character == '"') target.write('"');
            target.write(character);
        }
        target.write('"');
    }

    private static boolean isSpace(int codePoint) {

        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    @Override public void close() throws IOException {
        temporaryFile.close();
    }
}
