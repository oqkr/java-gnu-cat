package me.oqkr.gnu.coreutils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** An implementation of GNU core utils `cat` command. */
public class Cat {
  private static final String NAME = "cat";
  private static final String SHORT_USAGE = "cat [OPTION]... [FILE]...";
  private static final String HELP_HEADER =
      "Concatenate FILE(s) to standard output.\n\n"
          + "With no FILE, or when FILE is -, read standard input.\n\n";

  private static final String HELP_FOOTER =
      "\nExamples:\n"
          + "  cat f - g  Output f's contents, then standard input, then g's contents.\n"
          + "  cat        Copy standard input to standard output.\n";

  private final Options options = createOptions();

  public static void main(final String[] args) {
    new Cat().runAndHandleErrors(args);
  }

  private Stream<String> createLineStreamFromFiles(final String[] paths) throws IOException {
    var lineStream = Stream.<String>empty();
    for (final var path : paths) {
      final var inputStream = "-".equals(path) ? System.in : Files.newInputStream(Path.of(path));
      lineStream =
          Stream.concat(lineStream, new BufferedReader(new InputStreamReader(inputStream)).lines());
    }
    return lineStream;
  }

  private Function<String, String> createLineTransformFunction(final CommandLine commandLine) {
    final var lineTransformer = new LineTransformer();
    var function = Function.<String>identity();
    if (commandLine.hasOption("e")) function = function.andThen(lineTransformer::dashE);
    if (commandLine.hasOption("show-all")) function = function.andThen(lineTransformer::showAll);
    if (commandLine.hasOption("show-ends")) function = function.andThen(lineTransformer::showEnds);
    if (commandLine.hasOption("show-tabs")) function = function.andThen(lineTransformer::showTabs);
    if (commandLine.hasOption("t")) function = function.andThen(lineTransformer::dashT);
    if (commandLine.hasOption("u")) function = function.andThen(lineTransformer::dashU);
    if (commandLine.hasOption("show-nonprinting"))
      function = function.andThen(lineTransformer::showNonPrinting);

    // Do these last so another transformer won't change their output.
    if (commandLine.hasOption("number-nonblank")) {
      function = function.andThen(lineTransformer::numberNonBlank);
    } else if (commandLine.hasOption("number")) {
      function = function.andThen(lineTransformer::number);
    }
    return function;
  }

  private Predicate<String> createConsecutiveBlankLineFilter(final CommandLine commandLine) {
    final Predicate<String> doNothing = s -> true;
    final var squeezeBlank = Predicate.not(new LineTransformer()::isSqueezableBlank);
    return commandLine.hasOption("squeeze-blank") ? squeezeBlank : doNothing;
  }

  private Options createOptions() {
    return new Options()
        .addOption("a", "show-all", false, "equivalent to -vET")
        .addOption("b", "number-nonblank", false, "number nonempty output lines, overrides -n")
        .addOption("e", "equivalent to -vE")
        .addOption("E", "show-ends", false, "display $ at end of each line")
        .addOption("n", "number", false, "number all output lines")
        .addOption("s", "squeeze-blank", false, "suppress repeated empty output lines")
        .addOption("t", "equivalent to -vT")
        .addOption("T", "show-tabs", false, "display TAB characters as ^I")
        .addOption("u", "(ignored)")
        .addOption("v", "show-nonprinting", false, "use ^ and M- notation, except for LFD and TAB")
        .addOption(Option.builder().longOpt("help").desc("show this help and exit").build())
        .addOption(Option.builder().longOpt("version").desc("show version info and exit").build());
  }

  private void displayError(final Exception e) {
    System.err.println(NAME + ": " + e);
  }

  private void displayErrorAndQuit(final Exception e) {
    displayError(e);
    displayShortUsage();
    System.exit(1);
  }

  private void displayHelp() {
    new HelpFormatter().printHelp(SHORT_USAGE, HELP_HEADER, options, HELP_FOOTER, false);
  }

  private void displayHelpAndQuit() {
    displayHelp();
    System.exit(0);
  }

  private void displayShortUsage() {
    System.err.println(NAME + ": " + SHORT_USAGE);
  }

  private void displayVersion() {
    System.out.println(NAME + " " + getVersion());
  }

  private void displayVersionAndQuit() {
    displayVersion();
    System.exit(0);
  }

  private String[] getFileNamesFromCommandLine(final CommandLine commandLine) {
    final var positionalArgs = commandLine.getArgs();
    return positionalArgs.length > 0 ? positionalArgs : new String[] {"-"};
  }

  private Properties getProjectProperties() {
    final var properties = new Properties();
    try (var stream = getClass().getResourceAsStream("/project.properties")) {
      properties.load(stream);
    } catch (IOException e) {
      throw new ImplementationError("can't load properties file: " + e.getMessage());
    }
    return properties;
  }

  private String getVersion() {
    final var version = getProjectProperties().getProperty("version");
    if (version == null) throw new ImplementationError("no version info in properties file");
    return version;
  }

  private CommandLine parseArgs(final String[] args) throws ParseException {
    return new DefaultParser().parse(options, args);
  }

  private void run(final String[] args) throws ParseException, IOException {
    final var commandLine = parseArgs(args);
    if (commandLine.hasOption("help")) displayHelpAndQuit();
    if (commandLine.hasOption("version")) displayVersionAndQuit();

    final var lineStream = createLineStreamFromFiles(getFileNamesFromCommandLine(commandLine));
    final var transformLines = createLineTransformFunction(commandLine);
    final var dropConsecutiveBlankLines = createConsecutiveBlankLineFilter(commandLine);
    lineStream.filter(dropConsecutiveBlankLines).map(transformLines).forEach(System.out::println);
  }

  private void runAndHandleErrors(final String[] args) {
    try {
      run(args);
    } catch (Exception e) {
      displayErrorAndQuit(e);
    }
  }

  public class ImplementationError extends Error {
    ImplementationError(final String message) {
      super(message);
    }
  }

  private class LineTransformer {
    private int currentLineNumber;
    private boolean previousLineWasEmpty;

    String dashE(final String line) {
      return showEnds(showNonPrinting(line));
    }

    String dashT(final String line) {
      return showTabs(showNonPrinting(line));
    }

    // -u is ignored
    String dashU(final String line) {
      return line;
    }

    String number(final String line) {
      return "\t" + ++currentLineNumber + "\t" + line;
    }

    String numberNonBlank(final String line) {
      return line.isEmpty() ? line : number(line);
    }

    String showAll(final String line) {
      return showTabs(showEnds(showNonPrinting(line)));
    }

    String showEnds(final String line) {
      return line + "$";
    }

    // TODO: Not yet implemented
    String showNonPrinting(final String line) {
      return line;
    }

    String showTabs(final String line) {
      return line.replace("\t", "^I");
    }

    boolean isSqueezableBlank(final String line) {
      final var currentLineIsEmpty = line.isEmpty();
      final var answer = previousLineWasEmpty && currentLineIsEmpty;
      previousLineWasEmpty = currentLineIsEmpty;
      return answer;
    }
  }
}
