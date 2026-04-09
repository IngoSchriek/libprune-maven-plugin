package io.github.ingoschriek.libprune.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitRestorerTest {

    @Test
    void classNameFromPathExtractsCorrectly() throws Exception {
        // GitRestorer needs a git repo, but classNameFromPath is a pure method.
        // We test it via a minimal instance pointing to /tmp.
        GitRestorer restorer = new GitRestorer(new java.io.File("/tmp"));

        assertEquals("MyService",
                restorer.classNameFromPath("src/main/java/com/example/MyService.java"));
        assertEquals("Foo",
                restorer.classNameFromPath("module/src/main/java/com/example/Foo.java"));
        assertEquals("Bar",
                restorer.classNameFromPath("Bar.java"));
    }
}
