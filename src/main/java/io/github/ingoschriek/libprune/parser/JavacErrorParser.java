package io.github.ingoschriek.libprune.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses javac compilation errors to extract missing classes and packages,
 * classifying each as needed by production code or only by test code.
 */
public final class JavacErrorParser implements ErrorParser {

    private static final Pattern SYMBOL_PATTERN =
            Pattern.compile("symbol:\\s*(?:class|variable|method|static)\\s+(\\w+)");

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("package\\s+([\\w.]+)\\s+does not exist");

    private static final Pattern ERROR_FILE_PATTERN =
            Pattern.compile("(\\S+\\.java):\\[\\d+");

    @Override
    public ParseResult parse(String compilerOutput) {
        Map<MissingSymbol, Boolean> seen = new HashMap<>();

        String[] lines = compilerOutput.split("\n");
        String currentFile = null;

        for (String line : lines) {
            Matcher fileMatcher = ERROR_FILE_PATTERN.matcher(line);
            if (fileMatcher.find()) {
                currentFile = fileMatcher.group(1);
            }

            boolean isTest = isTestFile(currentFile);

            Matcher symbolMatcher = SYMBOL_PATTERN.matcher(line);
            if (symbolMatcher.find()) {
                String name = symbolMatcher.group(1);
                if (Character.isUpperCase(name.charAt(0))) {
                    mergeTestFlag(seen, new MissingSymbol(name, MissingSymbol.Kind.CLASS), isTest);
                }
            }

            Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);
            if (packageMatcher.find()) {
                String pkg = packageMatcher.group(1);
                mergeTestFlag(seen, new MissingSymbol(pkg, MissingSymbol.Kind.PACKAGE), isTest);
            }
        }

        Set<MissingSymbol> production = new HashSet<>();
        Set<MissingSymbol> testOnly = new HashSet<>();
        for (Map.Entry<MissingSymbol, Boolean> entry : seen.entrySet()) {
            if (entry.getValue()) {
                testOnly.add(entry.getKey());
            } else {
                production.add(entry.getKey());
            }
        }

        return new ParseResult(production, testOnly);
    }

    private static boolean isTestFile(String filePath) {
        return filePath != null && filePath.contains("/src/test/");
    }

    private static void mergeTestFlag(Map<MissingSymbol, Boolean> seen,
                                       MissingSymbol sym, boolean isTest) {
        Boolean existing = seen.get(sym);
        if (existing == null) {
            seen.put(sym, isTest);
        } else if (!isTest) {
            seen.put(sym, false);
        }
    }
}
