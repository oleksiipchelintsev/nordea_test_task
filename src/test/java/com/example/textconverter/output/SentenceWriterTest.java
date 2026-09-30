package com.example.textconverter.output;

import com.example.textconverter.TestDirectory;
import com.example.textconverter.model.Sentence;
import com.example.textconverter.sorting.InMemoryWordSorter;
import com.example.textconverter.model.SentenceBatch;
import org.junit.jupiter.api.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SentenceWriterTest {
    @Test
    void xmlRejectsForbiddenCharactersFromDirectCallers() throws Exception {
        for (String invalid : List.of("a\u0000b", "\ud800")) {
            var batch = new SentenceBatch();
            batch.accept(new Sentence(1), invalid);
            new InMemoryWordSorter().sort(batch);
            var words = batch.sentences().values().iterator().next();
            try (var writer = new XmlSentenceWriter(new StringWriter())) {
                writer.start();
                assertThrows(com.example.textconverter.error.OutputWriteException.class,
                        () -> writer.writeSentence(new Sentence(1), words));
            }
        }
    }

    @Test
    void xmlEscapesSpecialCharactersAndPreservesUnicode() throws Exception {
        var batch = new SentenceBatch();
        String word = "a&<>'\"停";
        batch.accept(new Sentence(1), word);
        var output = new StringWriter();
        new InMemoryWordSorter().sort(batch);
            var words = batch.sentences().values().iterator().next();
        try (var writer = new XmlSentenceWriter(output)) {
            writer.start();
            writer.writeSentence(new Sentence(1), words);
            writer.finish();
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(output.toString())));
        assertEquals(word, document.getElementsByTagName("word").item(0).getTextContent());
        assertTrue(output.toString().endsWith("</text>\n"));
    }

    @Test
    void csvEscapesFieldsWithoutBuildingWholeRows() throws Exception {
        var result = new StringWriter();
        for (String field : List.of("plain", "a,b", "a\"b", "a\nb", " a", "b ", "\u00a0x")) {
            CsvSentenceWriter.writeField(result, field);
            result.write('|');
        }
        assertEquals("plain|\"a,b\"|\"a\"\"b\"|\"a\nb\"|\" a\"|\"b \"|\"\u00a0x\"|", result.toString());
    }

    @Test
    void csvHeaderUsesLongestSentenceEvenWhenItIsLast() throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new StringWriter();
            try (var writer = new CsvSentenceWriter(output, directory.path)) {
                writer.start();
                for (int i = 1; i <= 2; i++) {
                    var batch = new SentenceBatch();
                    for (int j = 0; j < i; j++) batch.accept(new Sentence(i), "a");
                    new InMemoryWordSorter().sort(batch);
                    writer.writeBatch(batch);
                }
                assertEquals("", output.toString(), "CSV must not emit before header width is known");
                writer.finish();
            }
            assertEquals(", Word 1, Word 2\nSentence 1, a\nSentence 2, a, a\n", output.toString());
        }
    }
}
