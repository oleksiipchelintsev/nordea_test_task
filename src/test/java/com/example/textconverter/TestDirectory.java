package com.example.textconverter;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TestDirectory implements AutoCloseable {
    public final Path path = Files.createTempDirectory("converter-test-");
    public TestDirectory() throws IOException { }
    public void assertEmpty() throws IOException {
        try (var files = Files.list(path)) {
            assertEquals(0, files.count(), "Temporary resources leaked");
        }
    }
    @Override public void close() throws IOException {
        try (var files = Files.newDirectoryStream(path)) {
            for (Path file : files) Files.delete(file);
        }
        Files.delete(path);
    }
}
