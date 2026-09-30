package com.example.textconverter.output;

import com.example.textconverter.model.Sentence;
import com.example.textconverter.model.SentenceBatch;
import java.util.List;
import java.io.IOException;

/** Writes sentences without taking ownership of the output stream. */
public interface SentenceWriter extends AutoCloseable {
    void start() throws IOException;
    void writeSentence(Sentence sentence, List<String> words) throws IOException;
    default void writeBatch(SentenceBatch batch) throws IOException {
        for (var entry : batch.sentences().entrySet()) {
            writeSentence(entry.getKey(), entry.getValue());
        }
    }

    void finish() throws IOException;
    @Override void close() throws IOException;
}
