package com.example.textconverter.model;

/** Identifies a sentence within one conversion. */
public record Sentence(long ordinal) {
    public Sentence {
        if (ordinal < 1) throw new IllegalArgumentException("Sentence ordinal must be positive");
    }
}
