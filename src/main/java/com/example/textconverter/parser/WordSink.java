package com.example.textconverter.parser;

import com.example.textconverter.model.Sentence;
import java.io.IOException;

/** Receives a word together with the sentence it belongs to. */
@FunctionalInterface
public interface WordSink {
    void accept(Sentence sentence, String word) throws IOException;
}
