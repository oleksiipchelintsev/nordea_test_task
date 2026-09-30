package com.example.textconverter.output;

import com.example.textconverter.error.OutputWriteException;
import com.example.textconverter.model.Sentence;
import java.util.List;
import javax.xml.stream.*;
import java.io.IOException;
import java.io.Writer;

/** Streams XML using StAX. */
public final class XmlSentenceWriter implements SentenceWriter {
    private final Writer output;
    private final XMLStreamWriter xml;

    public XmlSentenceWriter(Writer output) throws IOException {
        this.output = output;
        try {
            xml = XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(output);
        } catch (XMLStreamException exception) {
            throw failure(exception);
        }
    }

    @Override public void start() throws IOException {
        output.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        try {
            xml.writeStartElement("text");
            xml.writeCharacters("\n");
        } catch (XMLStreamException exception) { throw failure(exception); }
    }

    @Override public void writeSentence(Sentence sentence, List<String> words) throws IOException {
        try {
            xml.writeStartElement("sentence");
            for (String word : words) {
                validateXmlCharacters(word);
                xml.writeStartElement("word");
                xml.writeCharacters(word);
                xml.writeEndElement();
            }
            xml.writeEndElement();
            xml.writeCharacters("\n");
        } catch (XMLStreamException exception) { throw failure(exception); }
    }

    @Override public void finish() throws IOException {
        try {
            xml.writeEndElement();
            xml.writeCharacters("\n");
            xml.flush();
        } catch (XMLStreamException exception) { throw failure(exception); }
    }

    @Override public void close() throws IOException {
        try { xml.close(); }
        catch (XMLStreamException exception) { throw failure(exception); }
    }

    private static OutputWriteException failure(XMLStreamException exception) {
        return new OutputWriteException("Cannot write XML output", exception);
    }

    private static void validateXmlCharacters(String word) throws OutputWriteException {

        for (int offset = 0; offset < word.length();) {

            int codePoint = word.codePointAt(offset);

            if (!isValidXml10Character(codePoint)) {
                throw new OutputWriteException("Word contains a character forbidden in XML 1.0");
            }

            offset += Character.charCount(codePoint);
        }
    }

    // XML 1.0 Char ranges exclude null, unpaired surrogates, U+FFFE and U+FFFF.
    private static boolean isValidXml10Character(int codePoint) {
        return codePoint == 0x09
                || codePoint == 0x0A
                || codePoint == 0x0D

                || codePoint >= 0x20 && codePoint <= 0xD7FF

                || codePoint >= 0xE000 && codePoint <= 0xFFFD

                || codePoint >= 0x10000 && codePoint <= 0x10FFFF;
    }
}
