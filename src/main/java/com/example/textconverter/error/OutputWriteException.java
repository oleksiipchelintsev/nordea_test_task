package com.example.textconverter.error;

import java.io.IOException;

public final class OutputWriteException extends IOException {
    public OutputWriteException(String message) { super(message); }
    public OutputWriteException(String message, Throwable cause) { super(message, cause); }
}
