package io.github.ingoschriek.libprune.parser;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses javac compilation errors to extract missing classes and packages.
 *
 * <p>Supported error patterns:
 * <ul>
 *   <li>{@code symbol: class Foo} / {@code symbol: variable Foo} / {@code symbol: method foo}</li>
 *   <li>{@code package com.example.foo does not exist}</li>
 * </ul>
 */
public final class JavacErrorParser implements ErrorParser {

    private static final Pattern SYMBOL_PATTERN =
            Pattern.compile("symbol:\\s*(?:class|variable|method|static)\\s+(\\w+)");

    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("package\\s+([\\w.]+)\\s+does not exist");

    @Override
    public Set<MissingSymbol> parse(String compilerOutput) {
        Set<MissingSymbol> symbols = new HashSet<>();

        Matcher symbolMatcher = SYMBOL_PATTERN.matcher(compilerOutput);
        while (symbolMatcher.find()) {
            String name = symbolMatcher.group(1);
            if (Character.isUpperCase(name.charAt(0))) {
                symbols.add(new MissingSymbol(name, MissingSymbol.Kind.CLASS));
            }
        }

        Matcher packageMatcher = PACKAGE_PATTERN.matcher(compilerOutput);
        while (packageMatcher.find()) {
            String pkg = packageMatcher.group(1);
            symbols.add(new MissingSymbol(pkg, MissingSymbol.Kind.PACKAGE));
        }

        return symbols;
    }
}
