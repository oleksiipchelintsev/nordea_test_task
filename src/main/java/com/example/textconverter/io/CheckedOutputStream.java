package com.example.textconverter.io;

import com.example.textconverter.error.OutputWriteException;
import java.io.*;

/** Reports write failures, including errors swallowed by PrintStream. */
public final class CheckedOutputStream extends OutputStream {
    private final OutputStream target;
    public CheckedOutputStream(OutputStream target) { this.target = target; }

    @Override public void write(int value) throws IOException {
        try { target.write(value); }
        catch (IOException cause) { throw new OutputWriteException("Cannot write output", cause); }
    }
    @Override public void write(byte[] bytes, int start, int length) throws IOException {
        try { target.write(bytes, start, length); }
        catch (IOException cause) { throw new OutputWriteException("Cannot write output", cause); }
    }
    @Override public void flush() throws IOException {
        try { target.flush(); }
        catch (IOException cause) { throw new OutputWriteException("Cannot flush output", cause); }

        // PrintStream records errors instead of throwing IOException.
        if (target instanceof PrintStream stream && stream.checkError()) {
            throw new OutputWriteException("Output stream reported a write error");
        }
    }
}
