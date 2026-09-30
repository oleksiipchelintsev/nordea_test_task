package com.example.textconverter.error;

public final class InvalidArgumentsException extends Exception {
    public InvalidArgumentsException(String message) { super(message); }
    public InvalidArgumentsException(String message, Throwable cause) { super(message, cause); }
}
