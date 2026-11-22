package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkDirectoryTest {

    private Path folder;

    @BeforeEach
    void setUp(@TempDir final Path tempDir) {
        folder = tempDir;
    }

    @Test
    void testInstalledJdksListsOnlyJdkFoldersFromOldestToNewest() throws IOException {
        Files.createDirectories(folder.resolve("jdk-25.0.4+7"));
        Files.createDirectories(folder.resolve("jdk-9"));
        Files.createDirectories(folder.resolve("jdk-21.0.2"));
        Files.createDirectories(folder.resolve("tools"));
        Files.writeString(folder.resolve("jdk-notes.txt"), "not a folder");

        assertEquals(List.of("jdk-9", "jdk-21.0.2", "jdk-25.0.4+7"), new JdkDirectory(folder).installedJdks());
    }

    @Test
    void testInstalledJdksOfMissingFolderIsEmpty() throws IOException {
        assertEquals(List.of(), new JdkDirectory(folder.resolve("missing")).installedJdks());
    }

    @Test
    void testResolve() {
        assertEquals(folder.resolve("jdk-9"), new JdkDirectory(folder).resolve("jdk-9"));
    }
}
