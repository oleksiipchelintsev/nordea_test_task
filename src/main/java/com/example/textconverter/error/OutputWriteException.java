package com.example.textconverter.error;

import java.io.IOException;

public final class OutputWriteException extends IOException {
    private static final long serialVersionUID = 1L;
    public OutputWriteException(String message) { super(message); }
    public OutputWriteException(String message, Throwable cause) { super(message, cause); }
}
