package com.example.textconverter.parser;

import com.example.textconverter.model.SentenceBatch;
import java.io.IOException;
import java.io.Reader;

/** Reads complete sentences into a working batch. */
public final class SentenceBatchReader {
    private static final int WORDS_PER_BATCH = 4096;
    private static final int CHARACTERS_PER_BATCH = 65_536;
    private final SentenceReader reader;

    public SentenceBatchReader(Reader input) { reader = new SentenceReader(input); }

    public SentenceBatch readBatch() throws IOException {
        var batch = new SentenceBatch();
        do {
            if (reader.readSentence(batch) == null) break;
            // Flush targets
        } while (batch.wordCount() < WORDS_PER_BATCH
                && batch.characterCount() < CHARACTERS_PER_BATCH);
        return batch.isEmpty() ? null : batch;
    }
}
