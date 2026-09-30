package com.example.textconverter.cli;

import com.example.textconverter.error.InvalidArgumentsException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Parses the output format and optional CSV temporary directory. */
public record CliOptions(OutputFormat format, Path tempDirectory) {
    public static CliOptions parse(String[] args) throws InvalidArgumentsException {
        OutputFormat format = null;
        Path temp = null;

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            String option = args[i];

            if (!seen.add(option)) throw new InvalidArgumentsException("Duplicate option");
            switch (option) {
                case "--format", "--temp-dir" -> {

                    if (++i == args.length || args[i].startsWith("--")) {
                        throw new InvalidArgumentsException("Missing option value");
                    }
                    if (option.equals("--format")) {

                        format = switch (args[i]) {
                            case "xml" -> OutputFormat.XML;
                            case "csv" -> OutputFormat.CSV;
                            default -> throw new InvalidArgumentsException("Format must be xml or csv");
                        };
                    } else {
                        try {

                            temp = Path.of(args[i]);
                        } catch (InvalidPathException exception) {
                            throw new InvalidArgumentsException("Invalid temporary directory path", exception);
                        }
                    }
                }
                default -> throw new InvalidArgumentsException("Unknown or unsupported argument");
            }
        }

        if (format == null) throw new InvalidArgumentsException("--format is required (xml or csv)");
        return new CliOptions(format, temp);
    }
}
