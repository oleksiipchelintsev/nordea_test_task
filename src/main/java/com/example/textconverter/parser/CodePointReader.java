package com.example.textconverter.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/** Reads Unicode code points with one code point of lookahead. */
final class CodePointReader {

    private static final int EMPTY = -2;
    private final Reader input;
    private int pendingUnit = EMPTY;
    private int lookahead = EMPTY;

    CodePointReader(Reader input) {
        Objects.requireNonNull(input, "input");
        this.input = input instanceof BufferedReader ? input : new BufferedReader(input, 8192);
    }

    int peek() throws IOException {
        if (lookahead == EMPTY) {
            lookahead = read();
        }
        return lookahead;
    }

    int read() throws IOException {
        if (lookahead != EMPTY) {
            int result = lookahead;
            lookahead = EMPTY;
            return result;
        }
        int first = readUnit();
        if (first == -1 || !Character.isHighSurrogate((char) first)) {
            return first;
        }
        int second = readUnit();
        if (second != -1 && Character.isLowSurrogate((char) second)) {
            return Character.toCodePoint((char) first, (char) second);
        }

        // Preserve a char that did not complete the surrogate pair.
        pendingUnit = second;
        return first;
    }

    private int readUnit() throws IOException {
        if (pendingUnit != EMPTY) {
            int result = pendingUnit;
            pendingUnit = EMPTY;
            return result;
        }
        return input.read();
    }
}
