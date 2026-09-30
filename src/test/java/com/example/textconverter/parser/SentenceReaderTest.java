package com.example.textconverter.parser;

import com.example.textconverter.model.Sentence;
import com.example.textconverter.sorting.InMemoryWordSorter;
import com.example.textconverter.model.SentenceBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SentenceReaderTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "Mary had a little lamb. Peter called for the wolf, and Aesop came. Cinderella likes shoes.",
        "  Mary   had a little  lamb  . \n\n Peter called for the wolf , and Aesop came . Cinderella likes shoes.."
    })
    void assignmentExamples(String text) throws IOException {
        assertEquals(List.of(List.of("a", "had", "lamb", "little", "Mary"),
                List.of("Aesop", "and", "called", "came", "for", "Peter", "the", "wolf"),
                List.of("Cinderella", "likes", "shoes")), sorted(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \t\r\n", "...!??。！？", ", : - () ' ’ 😀"})
    void emptyInputProducesNoEvents(String text) throws IOException {
        var reader = new SentenceReader(new StringReader(text));
        WordSink sink = (key, word) -> fail("Unexpected word: " + word);
        assertNull(reader.readSentence(sink));
        assertNull(reader.readSentence(sink));
    }

    @Test
    void abbreviationAndUnicode() throws IOException {
        assertEquals(List.of(
                List.of("couldn't", "Mr.", "say", "Young", "停在那儿"),
                List.of("isn't", "It", "you'd")),
                sorted("Mr. Young couldn't say: 停在那儿! (It isn’t ‘you’d’)."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mr", "Mrs", "Ms", "Dr", "Prof", "Sr", "Jr", "mr", "DR", "pRoF"})
    void titleAbbreviationsPreservePeriodAndSentenceBoundaries(String title) throws IOException {
        assertEquals(List.of(List.of(title + ".", "Young"), List.of("Last")),
                sorted(title + ". Young. Last."));
        assertEquals(List.of(List.of(title + ".")), sorted(title + "."));
        assertEquals(List.of(List.of(title), List.of("Young")), sorted(title + " . Young."));
    }

    @Test
    void unsupportedAbbreviationsAndTitleSuffixesDoNotSuppressSentenceEnd() throws IOException {
        assertEquals(List.of(List.of("etc"), List.of("XDr"), List.of("Next")),
                sorted("etc. XDr. Next."));
    }

    @Test
    void eofAndContiguousOrdinals() throws IOException {
        var reader = new SentenceReader(new StringReader("...Same?! Same. Last word"));
        WordSink discard = (key, word) -> { };
        for (int i = 1; i <= 3; i++) {
            Sentence sentence = reader.readSentence(discard);
            assertEquals(i, sentence.ordinal());
        }
        assertNull(reader.readSentence(discard));
    }

    @Test
    void supplementaryLettersAndCombiningMarksSurviveOneUnitReads() throws IOException {
        String text = "\u0301 Mr. A\u0301 you’d \uD801\uDC00。Next！";
        var reader = new SentenceReader(new OneCharacterReader(text));
        var words = new ArrayList<String>();
        reader.readSentence((key, word) -> words.add(word));
        assertEquals(4, words.size());
        assertEquals(List.of("Mr.", "A\u0301", "you'd", "\uD801\uDC00"), words);
        words.clear();
        reader.readSentence((key, word) -> words.add(word));
        assertEquals(List.of("Next"), words);
    }

    @Test
    void splitBufferAndBomPolicy() throws IOException {
        String word = "a".repeat(8191) + "\uD801\uDC00";
        assertEquals(List.of(List.of(word, "tail")), sorted("\ufeff" + word + "\ufefftail."));
    }

    @Test
    void decimalHyphenAndApostropheRules() throws IOException {
        assertEquals(List.of(List.of("3", "Price"), List.of("14", "known", "well")),
                sorted("Price 3.14 well-known."));
        assertEquals(List.of(List.of("a", "b", "can't", "word", "you'd")),
                sorted("a''b ‘word’ can’t you‘d"));
    }

    @Test
    void deliversWordBeforeSentenceOrInputEnds() {
        Reader input = new Reader() {
            int offset;
            @Override
            public int read(char[] buffer, int start, int length) throws IOException {
                if (offset == 6) throw new IOException("Rest of input unavailable");
                buffer[start] = "First ".charAt(offset++);
                return 1;
            }
            @Override public void close() { }
        };
        var delivered = new ArrayList<String>();
        var reader = new SentenceReader(input);
        assertThrows(IOException.class, () -> reader.readSentence((key, word) -> delivered.add(word)));
        assertEquals(List.of("First"), delivered);
    }

    @Test
    void propagatesSinkFailureAndStops() {
        var reader = new SentenceReader(new StringReader("first second."));
        var failure = new IOException("Disk full");
        assertSame(failure, assertThrows(IOException.class,
                () -> reader.readSentence((key, word) -> { throw failure; })));
    }

    @Test
    void doesNotCloseCallerReader() throws IOException {
        var input = new StringReader("a.") {
            @Override public void close() { fail("Caller owns the reader"); }
        };
        var reader = new SentenceReader(input);
        reader.readSentence((key, word) -> { });
        assertNull(reader.readSentence((key, word) -> { }));
    }

    @Test
    void acceptsLongWordWithoutAnArtificialLimit() throws IOException {
        String word = "a".repeat(70_000) + "’b";
        assertEquals(List.of(List.of("a".repeat(70_000) + "'b")), sorted(word));
    }

    private static List<List<String>> sorted(String text) throws IOException {
        var reader = new SentenceReader(new StringReader(text));
        var result = new ArrayList<List<String>>();
        while (true) {
            var batch = new SentenceBatch();
            Sentence sentence = reader.readSentence(batch);
            if (sentence == null) break;
            new InMemoryWordSorter().sort(batch);
            var words = batch.sentences().values().iterator().next();
            assertEquals(words.size(), batch.wordCount());
            result.add(words);
        }
        return result;
    }

    private static final class OneCharacterReader extends StringReader {
        OneCharacterReader(String text) { super(text); }
        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            return super.read(buffer, offset, Math.min(1, length));
        }
    }
}
