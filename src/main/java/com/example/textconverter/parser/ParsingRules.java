package com.example.textconverter.parser;

import java.util.Set;

final class ParsingRules {

    private static final Set<String> TITLE_ABBREVIATIONS =
            Set.of("mr", "mrs", "ms", "dr", "prof", "sr", "jr");

    private ParsingRules() { }

    static boolean startsWord(int codePoint) {

        return Character.isLetterOrDigit(codePoint);
    }

    static boolean continuesWord(int codePoint) {

        int type = Character.getType(codePoint);
        return startsWord(codePoint) || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK || type == Character.ENCLOSING_MARK;
    }

    static boolean isApostrophe(int codePoint) {

        return codePoint == 39 || codePoint == 0x2018 || codePoint == 0x2019;
    }

    static boolean isSentenceEnd(int codePoint) {

        return codePoint == '.' || codePoint == '!' || codePoint == '?'
                || codePoint == 0x3002 || codePoint == 0xff01 || codePoint == 0xff1f;
    }

    static boolean isAbbreviationBeforePeriod(CharSequence word) {
        return TITLE_ABBREVIATIONS.contains(word.toString().toLowerCase());
    }
}
