package com.example.textconverter;

import com.example.textconverter.cli.CliOptions;
import com.example.textconverter.error.InvalidArgumentsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
    private static final InputStream UNREADABLE = new InputStream() {
        @Override public int read() { throw new AssertionError("Input must not be read"); }
    };

    @ParameterizedTest
    @ValueSource(strings = {"", "--help", "-h", "--format", "--format json", "--format XML", "--format=xml",
            "--debug", "--format xml --format csv", "--format xml extra",
            "--format csv --debug", "--format csv --temp-dir",
            "--format csv --temp-dir a --temp-dir b"})
    void invalidArgumentsAreRejectedBeforeInput(String command) {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        String[] args = command.isEmpty() ? new String[0] : command.split(" ");
        assertEquals(2, Application.run(args, UNREADABLE, output, new PrintStream(errors)));
        assertEquals(0, output.size());
        assertTrue(errors.toString().startsWith("ARGUMENTS:"));
    }

    @Test
    void acceptsReorderedOptionsAndRejectsInvalidPath() throws Exception {
        var options = CliOptions.parse(new String[]{"--temp-dir", "a b", "--format", "csv"});
        assertEquals("a b", options.tempDirectory().toString());
        assertThrows(InvalidArgumentsException.class,
                () -> CliOptions.parse(new String[]{"--format", "xml", "--temp-dir", "\0"}));
    }

    @Test
    void invalidUtf8HasCode3AndCsvProducesNoOutput() throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new ByteArrayOutputStream();
            var errors = new ByteArrayOutputStream();
            assertEquals(3, Application.run(args("csv", directory),
                    new ByteArrayInputStream(new byte[]{(byte) 0xc3, 0x28}), output, new PrintStream(errors)));
            assertEquals(0, output.size());
            assertTrue(errors.toString().startsWith("INPUT:"));
            directory.assertEmpty();
        }
    }

    @Test
    void inaccessibleCsvTempFailsBeforeReadingOrWriting() throws Exception {
        try (var directory = new TestDirectory()) {
            var output = new ByteArrayOutputStream();
            var errors = new ByteArrayOutputStream();
            assertEquals(4, Application.run(new String[]{"--format", "csv", "--temp-dir",
                    directory.path.resolve("missing").toString()}, UNREADABLE, output, new PrintStream(errors)));
            assertEquals(0, output.size());
            assertTrue(errors.toString().contains("does not exist"));
            directory.assertEmpty();
        }
    }

    @Test
    void ioMessagesDoNotEchoStreamContent() throws Exception {
        try (var directory = new TestDirectory()) {
            InputStream broken = new InputStream() {
                @Override public int read() throws IOException { throw new IOException("SECRET_SENTINEL"); }
            };
            var errors = new ByteArrayOutputStream();
            assertEquals(4, Application.run(args("xml", directory), broken,
                    new ByteArrayOutputStream(), new PrintStream(errors)));
            assertFalse(errors.toString().contains("SECRET_SENTINEL"));
            assertTrue(errors.toString().contains("Cannot read input"));
            assertFalse(errors.toString().contains("at com.example"));
            directory.assertEmpty();
        }
    }

    @Test
    void outputAndPrintStreamErrorsCannotReportSuccess() throws Exception {
        try (var directory = new TestDirectory()) {
            OutputStream broken = new OutputStream() {
                @Override public void write(int value) throws IOException { throw new IOException("No space"); }
            };
            for (OutputStream target : new OutputStream[]{broken, new PrintStream(broken)}) {
                var errors = new ByteArrayOutputStream();
                assertEquals(4, Application.run(args("xml", directory),
                        new ByteArrayInputStream("Hello.".getBytes(StandardCharsets.UTF_8)),
                        target, new PrintStream(errors)));
                assertTrue(errors.toString().contains("Cannot write or flush output"));
                directory.assertEmpty();
            }
        }
    }

    @Test
    void callerStreamsAreNotClosed() throws Exception {
        try (var directory = new TestDirectory()) {
            var input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)) {
                @Override public void close() { fail("Input closed"); }
            };
            var output = new ByteArrayOutputStream() {
                @Override public void close() { fail("Output closed"); }
            };
            assertEquals(0, Application.run(args("csv", directory), input, output,
                    new PrintStream(new ByteArrayOutputStream())));
            assertTrue(output.toString().contains("hello"));
            directory.assertEmpty();
        }
    }

    private static String[] args(String format, TestDirectory directory) {
        return new String[]{"--format", format, "--temp-dir", directory.path.toString()};
    }
}
