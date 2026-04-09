package io.github.ingoschriek.libprune.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JavacErrorParserTest {

    private JavacErrorParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavacErrorParser();
    }

    @Test
    void parsesCannotFindSymbolClass() {
        String output = "[ERROR] /path/Foo.java:[10,5] cannot find symbol\n"
                + "  symbol:   class MyService\n"
                + "  location: class com.example.Foo\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.contains(new MissingSymbol("MyService", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void parsesCannotFindSymbolVariable() {
        String output = "[ERROR] /path/Foo.java:[14,51] cannot find symbol\n"
                + "  symbol:   variable HawaDefaultDigestProvider\n"
                + "  location: class com.example.Foo\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.contains(
                new MissingSymbol("HawaDefaultDigestProvider", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void ignoresLowercaseVariables() {
        String output = "  symbol:   variable someLocalVar\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.isEmpty());
    }

    @Test
    void parsesPackageDoesNotExist() {
        String output = "[ERROR] /path/Foo.java:[12,54] "
                + "package br.ufsc.labsec.valueobject.crypto.cmc.controls does not exist\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.contains(new MissingSymbol(
                "br.ufsc.labsec.valueobject.crypto.cmc.controls", MissingSymbol.Kind.PACKAGE)));
    }

    @Test
    void parsesMultipleErrors() {
        String output = "  symbol:   class Alpha\n"
                + "  symbol:   class Beta\n"
                + "  symbol:   variable Gamma\n"
                + "package com.example.util does not exist\n"
                + "  symbol:   variable localThing\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertEquals(4, result.size());
        assertTrue(result.contains(new MissingSymbol("Alpha", MissingSymbol.Kind.CLASS)));
        assertTrue(result.contains(new MissingSymbol("Beta", MissingSymbol.Kind.CLASS)));
        assertTrue(result.contains(new MissingSymbol("Gamma", MissingSymbol.Kind.CLASS)));
        assertTrue(result.contains(
                new MissingSymbol("com.example.util", MissingSymbol.Kind.PACKAGE)));
    }

    @Test
    void returnsEmptyForCleanOutput() {
        String output = "[INFO] BUILD SUCCESS\n[INFO] Total time: 2.5s\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.isEmpty());
    }

    @Test
    void deduplicatesSameSymbol() {
        String output = "  symbol:   class Foo\n"
                + "  symbol:   class Foo\n"
                + "  symbol:   class Foo\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertEquals(1, result.size());
    }

    @Test
    void parsesMethodSymbol() {
        String output = "  symbol:   method doSomething\n";

        Set<MissingSymbol> result = parser.parse(output);

        // lowercase method name is ignored
        assertTrue(result.isEmpty());
    }

    @Test
    void parsesStaticSymbol() {
        String output = "  symbol:   static MyConstant\n";

        Set<MissingSymbol> result = parser.parse(output);

        assertTrue(result.contains(new MissingSymbol("MyConstant", MissingSymbol.Kind.CLASS)));
    }
}
