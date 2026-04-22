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

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("MyService", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void parsesCannotFindSymbolVariable() {
        String output = "[ERROR] /path/Foo.java:[14,51] cannot find symbol\n"
                + "  symbol:   variable HawaDefaultDigestProvider\n"
                + "  location: class com.example.Foo\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("HawaDefaultDigestProvider", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void ignoresLowercaseVariables() {
        String output = "  symbol:   variable someLocalVar\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().isEmpty());
        assertTrue(result.getTestOnly().isEmpty());
    }

    @Test
    void parsesPackageDoesNotExist() {
        String output = "[ERROR] /path/Foo.java:[12,54] "
                + "package br.ufsc.labsec.valueobject.crypto.cmc.controls does not exist\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(new MissingSymbol(
                "br.ufsc.labsec.valueobject.crypto.cmc.controls", MissingSymbol.Kind.PACKAGE)));
    }

    @Test
    void parsesMultipleErrors() {
        String output = "  symbol:   class Alpha\n"
                + "  symbol:   class Beta\n"
                + "  symbol:   variable Gamma\n"
                + "package com.example.util does not exist\n"
                + "  symbol:   variable localThing\n";

        ParseResult result = parser.parse(output);

        assertEquals(4, result.getProduction().size());
        assertTrue(result.getProduction().contains(
                new MissingSymbol("Alpha", MissingSymbol.Kind.CLASS)));
        assertTrue(result.getProduction().contains(
                new MissingSymbol("Beta", MissingSymbol.Kind.CLASS)));
        assertTrue(result.getProduction().contains(
                new MissingSymbol("Gamma", MissingSymbol.Kind.CLASS)));
        assertTrue(result.getProduction().contains(
                new MissingSymbol("com.example.util", MissingSymbol.Kind.PACKAGE)));
    }

    @Test
    void returnsEmptyForCleanOutput() {
        String output = "[INFO] BUILD SUCCESS\n[INFO] Total time: 2.5s\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().isEmpty());
        assertTrue(result.getTestOnly().isEmpty());
    }

    @Test
    void deduplicatesSameSymbol() {
        String output = "  symbol:   class Foo\n"
                + "  symbol:   class Foo\n"
                + "  symbol:   class Foo\n";

        ParseResult result = parser.parse(output);

        assertEquals(1, result.getProduction().size());
    }

    @Test
    void parsesMethodSymbol() {
        String output = "  symbol:   method doSomething\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().isEmpty());
    }

    @Test
    void parsesStaticSymbol() {
        String output = "  symbol:   static MyConstant\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("MyConstant", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void classifiesTestFileErrorsAsTestOnly() {
        String output = "[ERROR] /project/src/test/java/com/example/FooTest.java:[5,1] "
                + "cannot find symbol\n"
                + "  symbol:   class DeletedHelper\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().isEmpty());
        assertTrue(result.getTestOnly().contains(
                new MissingSymbol("DeletedHelper", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void classifiesProductionFileErrorsAsProduction() {
        String output = "[ERROR] /project/src/main/java/com/example/Foo.java:[10,1] "
                + "cannot find symbol\n"
                + "  symbol:   class Bar\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("Bar", MissingSymbol.Kind.CLASS)));
        assertTrue(result.getTestOnly().isEmpty());
    }

    @Test
    void symbolNeededByBothProductionAndTestIsProduction() {
        String output = "[ERROR] /project/src/test/java/com/example/FooTest.java:[5,1] "
                + "cannot find symbol\n"
                + "  symbol:   class Shared\n"
                + "[ERROR] /project/src/main/java/com/example/Foo.java:[10,1] "
                + "cannot find symbol\n"
                + "  symbol:   class Shared\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("Shared", MissingSymbol.Kind.CLASS)));
        assertFalse(result.getTestOnly().contains(
                new MissingSymbol("Shared", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void mixedProductionAndTestOnlySymbols() {
        String output = "[ERROR] /project/src/main/java/com/example/App.java:[8,1] "
                + "cannot find symbol\n"
                + "  symbol:   class ProdDep\n"
                + "[ERROR] /project/src/test/java/com/example/AppTest.java:[12,1] "
                + "cannot find symbol\n"
                + "  symbol:   class TestHelper\n";

        ParseResult result = parser.parse(output);

        assertEquals(1, result.getProduction().size());
        assertTrue(result.getProduction().contains(
                new MissingSymbol("ProdDep", MissingSymbol.Kind.CLASS)));
        assertEquals(1, result.getTestOnly().size());
        assertTrue(result.getTestOnly().contains(
                new MissingSymbol("TestHelper", MissingSymbol.Kind.CLASS)));
    }

    @Test
    void testPackageDoesNotExistClassifiedBySourceFile() {
        String output = "[ERROR] /project/src/test/java/com/example/FooTest.java:[3,1] "
                + "package com.example.internal does not exist\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().isEmpty());
        assertTrue(result.getTestOnly().contains(
                new MissingSymbol("com.example.internal", MissingSymbol.Kind.PACKAGE)));
    }

    @Test
    void noFileContextDefaultsToProduction() {
        String output = "  symbol:   class OrphanSymbol\n";

        ParseResult result = parser.parse(output);

        assertTrue(result.getProduction().contains(
                new MissingSymbol("OrphanSymbol", MissingSymbol.Kind.CLASS)));
        assertTrue(result.getTestOnly().isEmpty());
    }
}
