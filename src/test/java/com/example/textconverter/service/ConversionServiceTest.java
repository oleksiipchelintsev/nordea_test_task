package com.example.textconverter.service;

import com.example.textconverter.TestDirectory;
import com.example.textconverter.cli.OutputFormat;
import com.example.textconverter.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ConversionServiceTest {
    @ParameterizedTest
    @EnumSource(OutputFormat.class)
    void suppliedReferenceMatchesIncludingCanonicalLayout(OutputFormat format) throws Exception {
        try (var directory = new TestDirectory();
             var input = Files.newInputStream(Path.of("samples", "small.in"))) {
            var output = new ByteArrayOutputStream();
            new ConversionService().convert(input, output, format, directory.path);
            String suffix = format.name().toLowerCase(java.util.Locale.ROOT);
            String expected = Files.readString(Path.of("samples", "small." + suffix))
                    .replace("\r\n", "\n").replace("&apos;", "'");

            if (!expected.endsWith("\n")) expected += "\n";
            assertEquals(expected, output.toString(StandardCharsets.UTF_8));
            directory.assertEmpty();
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat.class)
    void emptyAndBomInput(OutputFormat format) throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new ByteArrayOutputStream();
            new ConversionService().convert(new ByteArrayInputStream("\ufeff \r\n..!".getBytes(StandardCharsets.UTF_8)),
                    output, format, directory.path);
            String expected = format == OutputFormat.CSV ? "\n"
                    : "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<text>\n</text>\n";
            assertEquals(expected, output.toString(StandardCharsets.UTF_8));
            directory.assertEmpty();
        }
    }

    @Test
    void mapProcessingPreservesRepeatedSentencesAndOrder() throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new ByteArrayOutputStream();
            new ConversionService().convert(new ByteArrayInputStream("B a. ".repeat(300).getBytes(StandardCharsets.UTF_8)),
                    output, OutputFormat.CSV, directory.path);
            String[] lines = output.toString(StandardCharsets.UTF_8).split("\n");
            assertEquals(301, lines.length);
            for (int i = 1; i <= 300; i++) assertEquals("Sentence " + i + ", a, B", lines[i]);
            directory.assertEmpty();
        }
    }

    @Test
    void xmlDoesNotNeedAnExistingTemporaryDirectory() throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new ByteArrayOutputStream();
            new ConversionService().convert(new ByteArrayInputStream("hello.".getBytes(StandardCharsets.UTF_8)),
                    output, OutputFormat.XML, directory.path.resolve("missing"));
            assertTrue(output.toString(StandardCharsets.UTF_8).contains("<word>hello</word>"));
            directory.assertEmpty();
        }
    }

    @Test
    void csvCleansSpoolAfterInputFailureAndPreservesCause() throws Exception {
        try (var directory = new TestDirectory()) {
            var error = assertThrows(InvalidInputException.class, () -> new ConversionService().convert(
                    new ByteArrayInputStream(new byte[]{(byte) 0xff}), new ByteArrayOutputStream(),
                    OutputFormat.CSV, directory.path));
            assertNotNull(error.getCause());
            directory.assertEmpty();
        }
    }

    @Test
    void csvCleansSpoolAfterOutputFailure() throws Exception {
        try (var directory = new TestDirectory()) {
            var cause = new IOException("Broken output");
            var output = new OutputStream() {
                @Override public void write(int value) throws IOException { throw cause; }
            };
            var error = assertThrows(com.example.textconverter.error.OutputWriteException.class,
                    () -> new ConversionService().convert(
                            new ByteArrayInputStream("hello.".getBytes(StandardCharsets.UTF_8)),
                            output, OutputFormat.CSV, directory.path));
            assertSame(cause, error.getCause());
            directory.assertEmpty();
        }
    }
}
