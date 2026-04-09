package io.github.ingoschriek.libprune;

import io.github.ingoschriek.libprune.compiler.CompileResult;
import io.github.ingoschriek.libprune.compiler.CompileRunner;
import io.github.ingoschriek.libprune.git.GitRestorer;
import io.github.ingoschriek.libprune.parser.ErrorParser;
import io.github.ingoschriek.libprune.parser.JavacErrorParser;
import io.github.ingoschriek.libprune.parser.MissingSymbol;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Goal that iteratively restores deleted classes until compilation succeeds.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Run {@code mvn compile}</li>
 *   <li>Parse javac errors for missing classes/packages</li>
 *   <li>Restore matching files via {@code git restore}</li>
 *   <li>Repeat until build passes or max iterations reached</li>
 * </ol>
 *
 * <p>Usage: {@code mvn libprune:recover}
 */
@Mojo(name = "recover", requiresProject = true)
public class RecoverMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File projectDir;

    @Parameter(property = "libprune.maxIterations", defaultValue = "30")
    private int maxIterations;

    @Parameter(property = "libprune.timeoutSeconds", defaultValue = "300")
    private long timeoutSeconds;

    @Parameter(property = "libprune.compileArgs")
    private List<String> compileArgs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doRecover();
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Recovery failed: " + e.getMessage(), e);
        }
    }

    private void doRecover() throws Exception {
        GitRestorer git = new GitRestorer(projectDir);
        CompileRunner compiler = new CompileRunner(projectDir, timeoutSeconds, compileArgs);
        ErrorParser parser = new JavacErrorParser();

        int totalDeleted = git.getDeletedCount();
        getLog().info("Deleted classes available for restoration: " + totalDeleted);
        getLog().info("Packages with deleted files: " + git.getDeletedPackageCount());

        if (totalDeleted == 0) {
            getLog().info("No deleted classes found in git. Nothing to do.");
            return;
        }

        List<String> restored = new ArrayList<>();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            getLog().info("");
            getLog().info("=== ITERATION " + iteration + " : running mvn compile ===");

            CompileResult result = compiler.run();

            if (result.isSuccess()) {
                printSummary(restored, totalDeleted);
                return;
            }

            Set<MissingSymbol> missing = parser.parse(result.getOutput());
            if (missing.isEmpty()) {
                getLog().error("Compilation failed but no recognizable missing symbols found.");
                getLog().error("Last 20 lines of output:");
                printTail(result.getOutput(), 20);
                throw new MojoFailureException(
                        "Compilation failed with unrecognizable errors. Check output above.");
            }

            Map<String, String> toRestore = resolveFilesToRestore(missing, git);

            if (toRestore.isEmpty()) {
                getLog().error("Missing symbols detected but none match deleted files: " + missing);
                getLog().error("This may be an external dependency issue.");
                printTail(result.getOutput(), 20);
                throw new MojoFailureException(
                        "Missing symbols do not match any deleted files. Check dependencies.");
            }

            getLog().info("Files to restore this iteration: " + toRestore.size());
            TreeSet<String> sortedNames = new TreeSet<>(toRestore.keySet());
            for (String className : sortedNames) {
                String path = toRestore.get(className);
                boolean ok = git.restore(path);
                if (ok) {
                    restored.add(className);
                    getLog().info("  RESTORED: " + className + "  (" + path + ")");
                } else {
                    getLog().warn("  FAILED to restore: " + path);
                }
            }
        }

        getLog().warn("Reached max iterations (" + maxIterations + "). Check manually.");
        throw new MojoFailureException("Did not converge after " + maxIterations + " iterations.");
    }

    private Map<String, String> resolveFilesToRestore(Set<MissingSymbol> missing, GitRestorer git) {
        Map<String, String> toRestore = new HashMap<>();

        for (MissingSymbol symbol : missing) {
            if (symbol.getKind() == MissingSymbol.Kind.CLASS) {
                String path = git.findByClassName(symbol.getName());
                if (path != null) {
                    toRestore.put(symbol.getName(), path);
                }
            } else if (symbol.getKind() == MissingSymbol.Kind.PACKAGE) {
                List<String> paths = git.findByPackage(symbol.getName());
                for (String path : paths) {
                    String className = git.classNameFromPath(path);
                    if (git.findByClassName(className) != null) {
                        toRestore.put(className, path);
                    }
                }
            }
        }

        return toRestore;
    }

    private void printSummary(List<String> restored, int totalDeleted) {
        getLog().info("");
        getLog().info("BUILD SUCCESSFUL");
        getLog().info("Total classes restored: " + restored.size());

        if (!restored.isEmpty()) {
            getLog().info("");
            getLog().info("Restored classes:");
            TreeSet<String> sorted = new TreeSet<>(restored);
            for (String name : sorted) {
                getLog().info("  + " + name);
            }
        }

        getLog().info("");
        getLog().info("Classes that remained deleted: " + (totalDeleted - restored.size()));
    }

    private void printTail(String output, int lines) {
        String[] allLines = output.split("\n");
        int start = Math.max(0, allLines.length - lines);
        for (int i = start; i < allLines.length; i++) {
            getLog().error("  " + allLines[i]);
        }
    }
}
