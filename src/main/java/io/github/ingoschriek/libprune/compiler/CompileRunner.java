package io.github.ingoschriek.libprune.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs {@code mvn compile} as a subprocess and captures the output.
 */
public final class CompileRunner {

    private final File projectDir;
    private final long timeoutSeconds;
    private final List<String> extraArgs;

    public CompileRunner(File projectDir, long timeoutSeconds, List<String> extraArgs) {
        this.projectDir = projectDir;
        this.timeoutSeconds = timeoutSeconds;
        this.extraArgs = extraArgs != null ? extraArgs : List.of();
    }

    public CompileRunner(File projectDir) {
        this(projectDir, 300, List.of());
    }

    /**
     * Run {@code mvn compile} and return the result.
     */
    public CompileResult run() throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(findMvn());
        command.add("compile");
        command.add("-q");
        command.addAll(extraArgs);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(projectDir)
                .redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CompileResult(false, output + "\n[TIMEOUT after " + timeoutSeconds + "s]");
        }

        return new CompileResult(process.exitValue() == 0, output.toString());
    }

    private String findMvn() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Check for mvnw.cmd first, then mvn.cmd
            File mvnw = new File(projectDir, "mvnw.cmd");
            if (mvnw.exists()) return mvnw.getAbsolutePath();
            return "mvn.cmd";
        }
        File mvnw = new File(projectDir, "mvnw");
        if (mvnw.exists() && mvnw.canExecute()) return mvnw.getAbsolutePath();
        return "mvn";
    }
}
