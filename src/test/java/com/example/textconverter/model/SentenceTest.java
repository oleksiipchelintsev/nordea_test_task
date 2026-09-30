package com.example.textconverter.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SentenceTest {
    @Test void sentenceIdentityIsStableBeforeAndAfterWordsAreAdded() {
        var key = new Sentence(1);
        var batch = new SentenceBatch();
        batch.accept(key, "one");
        batch.accept(new Sentence(1), "two");
        assertEquals(1, batch.sentences().size());
        assertEquals(2, batch.sentences().get(key).size());
        assertNotEquals(new Sentence(2), key);
        assertEquals(key.hashCode(), new Sentence(1).hashCode());
    }

    @Test void rejectsInvalidOrdinal() {
        assertThrows(IllegalArgumentException.class, () -> new Sentence(0));
    }
}
