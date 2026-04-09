package io.github.ingoschriek.libprune.parser;

import java.util.Set;

/**
 * Parses compiler output to extract missing symbols.
 */
public interface ErrorParser {

    /**
     * Parse compiler output and return the set of missing symbols.
     *
     * @param compilerOutput the full stdout+stderr from the build tool
     * @return set of missing symbols (classes and packages)
     */
    Set<MissingSymbol> parse(String compilerOutput);
}
