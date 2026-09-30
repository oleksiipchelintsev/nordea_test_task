package com.example.textconverter;

import com.example.textconverter.cli.CliOptions;
import com.example.textconverter.error.*;
import com.example.textconverter.service.ConversionService;
import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/** Maps conversion failures to CLI exit codes. */
public final class Application {
    private Application() { }

    public static void main(String[] args) {
        int code = run(args, System.in, System.out, System.err);
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, InputStream input, OutputStream output, PrintStream errors) {
        try {
            CliOptions options = CliOptions.parse(args);
            new ConversionService().convert(input, output, options.format(), options.tempDirectory());
            return 0;
        } catch (InvalidArgumentsException exception) {
            return report(errors, 2, "ARGUMENTS", exception.getMessage());
        } catch (InvalidInputException exception) {
            return report(errors, 3, "INPUT", exception.getMessage());
        } catch (IOException exception) {

            return report(errors, 4, "IO", ioMessage(exception));
        } catch (RuntimeException exception) {
            return report(errors, 1, "INTERNAL", "Unexpected conversion failure");
        }
    }

    private static String ioMessage(IOException exception) {
        if (exception instanceof OutputWriteException) return "Cannot write or flush output";
        if (exception instanceof NoSuchFileException) return "File or directory does not exist";
        if (exception instanceof AccessDeniedException) return "Access denied to file or directory";
        return "Cannot read input or access temporary CSV data";
    }

    private static int report(PrintStream errors, int code, String category, String message) {
        errors.println(category + ": " + message);
        return code;
    }
}
