package com.example.textconverter.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Owns one temporary CSV file and removes it on close. */
public final class TemporaryCsvFile implements AutoCloseable {
    private final Path file;
    private Writer writer;

    public TemporaryCsvFile(Path directory) throws IOException {
        file = directory == null ? Files.createTempFile("text-converter-", ".csv")
                : Files.createTempFile(directory, "text-converter-", ".csv");
        try {
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        // Construction failed before the caller could take ownership of the file.
        } catch (IOException exception) {

            try { Files.deleteIfExists(file); }
            catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            throw exception;
        }
    }

    public Writer writer() { return writer; }

    public void finishWriting() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    public Reader openReader() throws IOException {
        return Files.newBufferedReader(file, StandardCharsets.UTF_8);
    }

    @Override public void close() throws IOException {
        IOException failure = null;
        try { finishWriting(); }
        catch (IOException exception) { failure = exception; }
        try { Files.deleteIfExists(file); }
        catch (IOException exception) {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }
}
