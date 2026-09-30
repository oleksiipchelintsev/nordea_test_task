package com.example.textconverter.model;

import com.example.textconverter.parser.WordSink;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups parsed words by sentence, preserving the order of first occurrence. */
public final class SentenceBatch implements WordSink {
    private final Map<Sentence, List<String>> sentences = new LinkedHashMap<>();
    private long wordCount;
    private long characterCount;

    @Override public void accept(Sentence sentence, String word) {
        sentences.computeIfAbsent(sentence, key -> new ArrayList<>()).add(word);
        wordCount++;
        characterCount += word.length();
    }

    public Map<Sentence, List<String>> sentences() {
        return Collections.unmodifiableMap(sentences);
    }

    public long wordCount() { return wordCount; }
    public long characterCount() { return characterCount; }
    public boolean isEmpty() { return sentences.isEmpty(); }
}
