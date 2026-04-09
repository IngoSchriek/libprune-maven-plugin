package io.github.ingoschriek.libprune.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MissingSymbolTest {

    @Test
    void equalSymbolsAreEqual() {
        MissingSymbol a = new MissingSymbol("Foo", MissingSymbol.Kind.CLASS);
        MissingSymbol b = new MissingSymbol("Foo", MissingSymbol.Kind.CLASS);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentKindsAreNotEqual() {
        MissingSymbol a = new MissingSymbol("com.example", MissingSymbol.Kind.CLASS);
        MissingSymbol b = new MissingSymbol("com.example", MissingSymbol.Kind.PACKAGE);

        assertNotEquals(a, b);
    }

    @Test
    void toStringShowsKindAndName() {
        MissingSymbol s = new MissingSymbol("MyClass", MissingSymbol.Kind.CLASS);

        assertEquals("CLASS:MyClass", s.toString());
    }
}
