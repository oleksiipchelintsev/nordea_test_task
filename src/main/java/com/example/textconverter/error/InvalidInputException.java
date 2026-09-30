package com.example.textconverter.error;

import java.io.IOException;

public final class InvalidInputException extends IOException {
    public InvalidInputException(String message, Throwable cause) { super(message, cause); }
}
