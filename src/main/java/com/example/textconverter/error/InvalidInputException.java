package com.example.textconverter.error;

import java.io.IOException;

public final class InvalidInputException extends IOException {
    private static final long serialVersionUID = 1L;
    public InvalidInputException(String message, Throwable cause) { super(message, cause); }
}
