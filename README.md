# Text Converter

Java command-line application that reads UTF-8 text from standard input, splits it
into sentences and words, sorts the words, and writes XML or CSV to standard output.
Sentence order, word casing and duplicate words are preserved.

## Build

Requires JDK 21+ and Maven 3.9+. Set `JAVA_HOME` to your JDK installation.

```sh
mvn clean verify
```

The executable JAR is `target/text-converter.jar`. There are no runtime dependencies.

## Run

From the project directory, in a POSIX shell or Windows Command Prompt:

```sh
java -Xmx32m -jar target/text-converter.jar --format xml < samples/small.in > result.xml
java -Xmx32m -jar target/text-converter.jar --format csv < samples/large.in > result.csv
```

In PowerShell, use `cmd /c` for input redirection:

```powershell
cmd /c 'java -Xmx32m -jar target/text-converter.jar --format xml < samples/small.in > result.xml'
cmd /c 'java -Xmx32m -jar target/text-converter.jar --format csv < samples/large.in > result.csv'
$LASTEXITCODE
```

`--format xml|csv` is required. `--temp-dir PATH` optionally selects an existing
directory for the CSV temporary file; XML does not use it. Unknown or repeated
options are rejected. Output redirection overwrites the destination file.

IntelliJ IDEA users can import the Maven project and run the saved `XML small sample`
or `CSV small sample` configurations with JDK 21+.

## Parsing and output

- Sentence endings: `.`, `!`, `?`, `。`, `！`, `？`. Empty sentences are skipped.
  A nonempty fragment at EOF is included.
- `Mr.`, `Mrs.`, `Ms.`, `Dr.`, `Prof.`, `Sr.` and `Jr.` are retained as abbreviations.
- Words contain Unicode letters, digits and following combining marks. Internal
  apostrophes are preserved; curly apostrophes are converted to straight ones.
  Other punctuation separates words. Decimal numbers and URLs have no special handling.
- Sorting is case-insensitive, with reverse natural string order as a tie-breaker
  to match the supplied examples.
- Output is UTF-8 with LF line endings. CSV uses `, ` between fields, a header based
  on the longest sentence, and no padding of shorter rows.

## Errors and resource use

Diagnostics go to standard error. A nonzero exit code means the output is incomplete
or the conversion did not finish successfully.

| Exit code | Meaning |
| --- | --- |
| 0 | Success |
| 1 | Unexpected application error |
| 2 | Invalid arguments |
| 3 | Invalid UTF-8 input |
| 4 | Input, output or temporary-file error |

Words are grouped by sentence in a `LinkedHashMap`, preserving input order. Each
batch is sorted and written before reading the next one. Batch targets trigger a
flush after a complete sentence; they do not reject or split long sentences. The
working batch, the largest sentence and its sorting buffers must fit in the heap. CSV stores rows in a temporary file until the header width is known.
The file is removed on normal completion and handled failures; abrupt process termination
may leave it behind.

Tests cover the supplied examples, parsing rules, output formatting, error handling,
and both output formats on `large.in` in a separate JVM with `-Xmx32m`.
The original assignment is in `assignment/`; supplied inputs and reference outputs are in `samples/`.
