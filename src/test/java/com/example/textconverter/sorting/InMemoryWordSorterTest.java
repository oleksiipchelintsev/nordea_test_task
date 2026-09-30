package com.example.textconverter.sorting;

import com.example.textconverter.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryWordSorterTest {
    @Test void sortsEachSentenceWithoutMixingWordsOrRemovingDuplicates() {
        var batch = new SentenceBatch();
        for (String word : List.of("Markets", "in", "In", "markets", "in")) batch.accept(new Sentence(1), word);
        batch.accept(new Sentence(2), "z");
        batch.accept(new Sentence(2), "a");
        new InMemoryWordSorter().sort(batch);
        assertEquals(List.of("in", "in", "In", "markets", "Markets"), batch.sentences().get(new Sentence(1)));
        assertEquals(List.of("a", "z"), batch.sentences().get(new Sentence(2)));
        assertEquals(List.of(new Sentence(1), new Sentence(2)), List.copyOf(batch.sentences().keySet()));
    }

    @Test void sortingDoesNotDependOnLocale() {
        Locale previous = Locale.getDefault();
        try {
            var english = sample();
            Locale.setDefault(Locale.ENGLISH);
            new InMemoryWordSorter().sort(english);
            var turkish = sample();
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            new InMemoryWordSorter().sort(turkish);
            assertEquals(english.sentences(), turkish.sentences());
        } finally { Locale.setDefault(previous); }
    }

    private static SentenceBatch sample() {
        var batch = new SentenceBatch();
        for (String word : List.of("I", "i", "Markets", "markets", "你", "停")) batch.accept(new Sentence(1), word);
        return batch;
    }
}
