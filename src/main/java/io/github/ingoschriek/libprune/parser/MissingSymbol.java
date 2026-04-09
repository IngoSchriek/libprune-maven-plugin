package io.github.ingoschriek.libprune.parser;

/**
 * Represents a symbol (class or package) that is missing from compilation.
 */
public final class MissingSymbol {

    public enum Kind {
        CLASS,
        PACKAGE
    }

    private final String name;
    private final Kind kind;

    public MissingSymbol(String name, Kind kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return kind + ":" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MissingSymbol)) return false;
        MissingSymbol that = (MissingSymbol) o;
        return name.equals(that.name) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + kind.hashCode();
    }
}
