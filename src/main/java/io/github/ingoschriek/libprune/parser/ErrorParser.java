package io.github.ingoschriek.libprune.parser;

/**
 * Parses compiler output to extract missing symbols.
 */
public interface ErrorParser {

    /**
     * Parse compiler output and classify missing symbols as production or test-only.
     *
     * @param compilerOutput the full stdout+stderr from the build tool
     * @return parse result with production and test-only symbol sets
     */
    ParseResult parse(String compilerOutput);
}
