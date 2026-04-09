package io.github.ingoschriek.libprune.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages deleted files via git and restores them on demand.
 */
public final class GitRestorer {

    private static final Pattern JAVA_PACKAGE_PATTERN =
            Pattern.compile("src/main/java/(.+)/[^/]+\\.java$");

    private final File projectDir;

    /** class name -> relative path */
    private final Map<String, String> deletedByClass;

    /** java package -> list of relative paths */
    private final Map<String, List<String>> deletedByPackage;

    public GitRestorer(File projectDir) throws IOException {
        this.projectDir = projectDir;
        this.deletedByClass = new HashMap<>();
        this.deletedByPackage = new HashMap<>();
        loadDeletedFiles();
    }

    private void loadDeletedFiles() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", "--diff-filter=D")
                .directory(projectDir)
                .redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.endsWith(".java")) continue;

                String className = Path.of(line).getFileName().toString().replace(".java", "");
                deletedByClass.put(className, line);

                Matcher m = JAVA_PACKAGE_PATTERN.matcher(line);
                if (m.find()) {
                    String pkg = m.group(1).replace("/", ".");
                    deletedByPackage.computeIfAbsent(pkg, k -> new ArrayList<>()).add(line);
                }
            }
        }
    }

    /**
     * @return number of deleted files available for restoration.
     */
    public int getDeletedCount() {
        return deletedByClass.size();
    }

    /**
     * @return number of packages that have deleted files.
     */
    public int getDeletedPackageCount() {
        return deletedByPackage.size();
    }

    /**
     * Find the relative path of a deleted class by its simple name.
     *
     * @return the path, or null if not found
     */
    public String findByClassName(String className) {
        return deletedByClass.get(className);
    }

    /**
     * Find all deleted file paths belonging to a Java package.
     *
     * @return list of relative paths, or empty list if none
     */
    public List<String> findByPackage(String packageName) {
        return deletedByPackage.getOrDefault(packageName, Collections.emptyList());
    }

    /**
     * Get the class name from a relative file path.
     */
    public String classNameFromPath(String relativePath) {
        String fileName = Path.of(relativePath).getFileName().toString();
        return fileName.replace(".java", "");
    }

    /**
     * Restore a file using {@code git restore}.
     *
     * @param relativePath the path relative to the project root
     * @return true if the restore command succeeded
     */
    public boolean restore(String relativePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "restore", relativePath)
                .directory(projectDir)
                .redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            String className = classNameFromPath(relativePath);
            deletedByClass.remove(className);
        }

        return exitCode == 0;
    }
}
