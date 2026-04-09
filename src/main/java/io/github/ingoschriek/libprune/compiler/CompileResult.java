package io.github.ingoschriek.libprune.compiler;

/**
 * Result of a Maven compilation attempt.
 */
public final class CompileResult {

    private final boolean success;
    private final String output;

    public CompileResult(boolean success, String output) {
        this.success = success;
        this.output = output;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }
}
