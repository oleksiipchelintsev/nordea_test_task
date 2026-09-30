package com.example.textconverter.integration;

import com.example.textconverter.Application;
import com.example.textconverter.TestDirectory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.xml.stream.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class CliMemoryTest {
    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv"})
    void acceptsSentenceAndWordBeyondFormerLimitsWith32MiBHeap(String format) throws Exception {
        try (var directory = new TestDirectory()) {
            Path input = directory.path.resolve("input.txt");
            Path result = directory.path.resolve("result.txt");
            Path errors = directory.path.resolve("errors.log");
            String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
            String java = Path.of(System.getProperty("java.home"), "bin", javaName).toString();
            String classes = Path.of(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            {
                Files.writeString(input, "a ".repeat(40_000) + "a".repeat(70_000) + ".");
                Process child = new ProcessBuilder(java, "-Xmx32m", "-cp", classes, Application.class.getName(),
                        "--format", format, "--temp-dir", directory.path.toString())
                        .redirectInput(input.toFile()).redirectOutput(result.toFile()).redirectError(errors.toFile()).start();
                try {
                    assertTrue(child.waitFor(30, TimeUnit.SECONDS), "CLI timed out");
                    assertEquals(0, child.exitValue(), Files.readString(errors));
                    assertEquals(0, Files.size(errors));
                    String content = Files.readString(result);
                    if (format.equals("xml")) {
                        assertEquals(40_001, content.split("<word>", -1).length - 1);
                        assertTrue(content.contains("<word>" + "a".repeat(70_000) + "</word>"));
                    } else {
                        String[] lines = content.split("\n");
                        assertEquals(40_002, lines[0].split(", ", -1).length);
                        assertEquals(40_002, lines[1].split(", ", -1).length);
                        assertTrue(lines[1].endsWith("a".repeat(70_000)));
                    }
                    try (var entries = Files.list(directory.path)) {
                        assertEquals(3, entries.count(), "Temporary workspace leaked");
                    }
                } finally {
                    if (child.isAlive()) child.destroyForcibly().waitFor();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv"})
    void completeCliProcessesLargeFileWith32MiBHeap(String format) throws Exception {
        try (var directory = new TestDirectory()) {
            Path result = directory.path.resolve("result." + format);
            Path errors = directory.path.resolve("errors.log");
            String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
            String java = Path.of(System.getProperty("java.home"), "bin", javaName).toString();
            String classes = Path.of(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
            Process child = new ProcessBuilder(java, "-Xmx32m", "-cp", classes, Application.class.getName(),
                    "--format", format, "--temp-dir", directory.path.toString())
                    .redirectInput(Path.of("samples", "large.in").toFile())
                    .redirectOutput(result.toFile()).redirectError(errors.toFile()).start();
            try {
                assertTrue(child.waitFor(60, TimeUnit.SECONDS), "CLI timed out");
                assertEquals(0, child.exitValue(), Files.readString(errors));
                assertEquals(0, Files.size(errors));
                if (format.equals("xml")) verifyXml(result);
                else verifyCsv(result);
                try (var entries = Files.list(directory.path)) {
                    assertEquals(2, entries.count(), "Temporary workspace leaked");
                }
            } finally {
                if (child.isAlive()) child.destroyForcibly().waitFor();
            }
        }
    }

    private static void verifyXml(Path file) throws Exception {
        long sentences = 0, words = 0;
        var factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        try (var input = Files.newInputStream(file)) {
            var xml = factory.createXMLStreamReader(input);
            try {
                while (xml.hasNext()) {
                    if (xml.next() == XMLStreamConstants.START_ELEMENT) {
                        if (xml.getLocalName().equals("sentence")) sentences++;
                        if (xml.getLocalName().equals("word")) words++;
                    }
                }
            } finally { xml.close(); }
        }
        assertEquals(692737, sentences);
        assertEquals(5433350, words);
    }

    private static void verifyCsv(Path file) throws Exception {
        long sentences = 0, words = 0;
        try (var input = Files.newBufferedReader(file)) {
            assertEquals(26, input.readLine().split(", ", -1).length);
            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = line.split(", ");
                assertEquals("Sentence " + (++sentences), fields[0]);
                words += fields.length - 1;
            }
        }
        assertEquals(692737, sentences);
        assertEquals(5433350, words);
    }
}
