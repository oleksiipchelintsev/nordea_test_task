package com.example.textconverter.parser;

import com.example.textconverter.model.Sentence;
import com.example.textconverter.sorting.InMemoryWordSorter;
import com.example.textconverter.model.SentenceBatch;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SampleFileTest {
    @Test
    void parsedAndSortedInputMatchesBothSuppliedReferences() throws Exception {
        List<List<String>> actual = new ArrayList<>();
        try (var input = Files.newBufferedReader(Path.of("samples", "small.in"), StandardCharsets.UTF_8)) {
            var parser = new SentenceReader(input);
            while (true) {
                var batch = new SentenceBatch();
                Sentence sentence = parser.readSentence(batch);
                if (sentence == null) break;
                new InMemoryWordSorter().sort(batch);
            var words = batch.sentences().values().iterator().next();
                assertEquals(words.size(), batch.wordCount());
                actual.add(words);
            }
        }

        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(Path.of("samples", "small.xml").toFile());
        var sentences = document.getElementsByTagName("sentence");
        List<List<String>> expectedXml = new ArrayList<>();
        for (int i = 0; i < sentences.getLength(); i++) {
            var nodes = sentences.item(i).getChildNodes();
            List<String> words = new ArrayList<>();
            for (int j = 0; j < nodes.getLength(); j++) {
                if (nodes.item(j).getNodeName().equals("word")) {
                    words.add(nodes.item(j).getTextContent());
                }
            }
            expectedXml.add(words);
        }

        var expectedCsv = Files.readAllLines(Path.of("samples", "small.csv"), StandardCharsets.UTF_8)
                .stream().skip(1)
                .map(line -> Arrays.stream(line.split(", ")).skip(1).toList())
                .toList();

        assertEquals(expectedXml, actual);
        assertEquals(expectedCsv, actual);
        assertEquals(13, actual.size());
        assertEquals(235, actual.stream().mapToInt(List::size).sum());
    }
}
