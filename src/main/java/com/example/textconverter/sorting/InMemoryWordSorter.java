package com.example.textconverter.sorting;

import com.example.textconverter.model.SentenceBatch;
import java.util.Comparator;

/** Sorts each sentence in a parsed batch without changing sentence order. */
public final class InMemoryWordSorter {
    private static final Comparator<String> WORD_COMPARATOR =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.reverseOrder());

    public void sort(SentenceBatch batch) {
        batch.sentences().values().forEach(words -> words.sort(WORD_COMPARATOR));
    }
}
