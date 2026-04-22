package io.github.ingoschriek.libprune.parser;

import java.util.Set;

public final class ParseResult {

    private final Set<MissingSymbol> production;
    private final Set<MissingSymbol> testOnly;

    public ParseResult(Set<MissingSymbol> production, Set<MissingSymbol> testOnly) {
        this.production = production;
        this.testOnly = testOnly;
    }

    public Set<MissingSymbol> getProduction() {
        return production;
    }

    public Set<MissingSymbol> getTestOnly() {
        return testOnly;
    }
}
