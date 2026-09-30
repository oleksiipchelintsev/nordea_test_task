package com.example.textconverter.parser;

import com.example.textconverter.model.Sentence;
import com.example.textconverter.model.SentenceBatch;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SentenceBatchReaderTest {
    @Test void groupsWordsByKeyAndPreservesSentenceOrder() {
        var batch = new SentenceBatch();
        batch.accept(new Sentence(1), "one");
        batch.accept(new Sentence(2), "two");
        batch.accept(new Sentence(1), "again");
        assertEquals(List.of(new Sentence(1), new Sentence(2)), List.copyOf(batch.sentences().keySet()));
        assertEquals(List.of("one", "again"), batch.sentences().get(new Sentence(1)));
        assertEquals(List.of("two"), batch.sentences().get(new Sentence(2)));
    }

    @Test void retainsIdenticalSentencesAndUnterminatedTail() throws Exception {
        var reader = new SentenceBatchReader(new StringReader("b a. b a. Last"));
        var batch = reader.readBatch();
        assertEquals(List.of(new Sentence(1), new Sentence(2), new Sentence(3)), List.copyOf(batch.sentences().keySet()));
        assertEquals(List.of("b", "a"), batch.sentences().get(new Sentence(1)));
        assertEquals(List.of("b", "a"), batch.sentences().get(new Sentence(2)));
        assertEquals(List.of("Last"), batch.sentences().get(new Sentence(3)));
        assertNull(reader.readBatch());
    }

    @Test void continuesOrdinalsAcrossBatches() throws Exception {
        var reader = new SentenceBatchReader(new StringReader("word. ".repeat(10000)));
        int batches = 0;
        long ordinal = 0;
        SentenceBatch batch;
        while ((batch = reader.readBatch()) != null) {
            batches++;
            for (var entry : batch.sentences().entrySet()) {
                assertEquals(++ordinal, entry.getKey().ordinal());
                assertEquals(List.of("word"), entry.getValue());
            }
        }
        assertTrue(batches > 1);
        assertEquals(10000, ordinal);
    }

    @Test void neverSplitsAnOversizedSentence() throws Exception {
        var reader = new SentenceBatchReader(new StringReader("a ".repeat(10000) + ". tail."));
        var batch = reader.readBatch();
        assertEquals(1, batch.sentences().size());
        assertEquals(10000, batch.sentences().get(new Sentence(1)).size());
        assertEquals(List.of("tail"), reader.readBatch().sentences().get(new Sentence(2)));
        assertNull(reader.readBatch());
    }

    @Test void acceptsWordsLargerThanBatchTarget() throws Exception {
        String word = "a".repeat(100000);
        var reader = new SentenceBatchReader(new StringReader(word + ". tail."));
        assertEquals(List.of(word), reader.readBatch().sentences().get(new Sentence(1)));
        assertEquals(List.of("tail"), reader.readBatch().sentences().get(new Sentence(2)));
        assertNull(reader.readBatch());
    }
}
