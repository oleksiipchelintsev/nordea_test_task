package com.example.textconverter.parser;

import com.example.textconverter.model.Sentence;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/** Emits words from one sentence at a time. Discard after a read or callback failure. */
public final class SentenceReader {
    private final CodePointReader input;
    private long ordinal;
    private boolean exhausted;

    public SentenceReader(Reader input) {
        this.input = new CodePointReader(input);
    }

    public Sentence readSentence(WordSink sink) throws IOException {

        Objects.requireNonNull(sink, "sink");
        return exhausted ? null : readNext(sink);
    }

    private Sentence readNext(WordSink sink) throws IOException {
        StringBuilder word = new StringBuilder();
        Sentence sentence = new Sentence(ordinal + 1);
        boolean hasWords = false;
        while (true) {
            int codePoint = input.read();

            if (codePoint == -1) {
                exhausted = true;
                hasWords = emit(word, sentence, sink) || hasWords;
                return complete(sentence, hasWords);
            }
            if (ParsingRules.startsWord(codePoint)
                    || (!word.isEmpty() && ParsingRules.continuesWord(codePoint))) {
                word.appendCodePoint(codePoint);

            } else if (!word.isEmpty() && ParsingRules.isApostrophe(codePoint)
                    && ParsingRules.startsWord(input.peek())) {

                word.appendCodePoint(39);
            } else if (codePoint == '.' && ParsingRules.isAbbreviationBeforePeriod(word)) {

                word.append('.');
                hasWords = emit(word, sentence, sink) || hasWords;
            } else {
                hasWords = emit(word, sentence, sink) || hasWords;

                if (ParsingRules.isSentenceEnd(codePoint) && hasWords) {
                    return complete(sentence, hasWords);
                }
            }
        }
    }

    private boolean emit(StringBuilder word, Sentence sentence, WordSink sink) throws IOException {
        if (word.isEmpty()) return false;
        sink.accept(sentence, word.toString());
        word.setLength(0);
        return true;
    }

    private Sentence complete(Sentence sentence, boolean hasWords) {
        if (!hasWords) return null;
        ordinal = sentence.ordinal();
        return sentence;
    }
}
