package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileOperationsTest {

    private Path folder;

    @BeforeEach
    void setUp(@TempDir final Path tempDir) {
        folder = tempDir;
    }

    @Test
    void testSha256() throws IOException {
        Path file = Files.writeString(folder.resolve("abc.txt"), "abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", FileOperations.sha256(file));
    }

    @Test
    void testUnzipExtractsFoldersAndFiles() throws IOException {
        Path archive = Files.write(folder.resolve("jdk.zip"), TestData.jdkZip("jdk-25.0.5+11"));

        FileOperations.unzip(archive, folder.resolve("out"));

        assertTrue(Files.isRegularFile(folder.resolve("out/jdk-25.0.5+11/bin/java.exe")));
        assertTrue(Files.isRegularFile(folder.resolve("out/jdk-25.0.5+11/release")));
    }

    @Test
    void testUnzipRejectsEntryOutsideTargetFolder() throws IOException {
        Path archive = Files.write(folder.resolve("evil.zip"), TestData.zip("../evil.txt"));

        assertThrows(IOException.class, () -> FileOperations.unzip(archive, folder.resolve("out")));
        assertFalse(Files.exists(folder.resolve("evil.txt")));
    }

    @Test
    void testDeleteRecursivelyDeletesFolderWithContent() throws IOException {
        Path tree = folder.resolve("tree");
        Files.createDirectories(tree.resolve("a/b"));
        Files.writeString(tree.resolve("a/b/file.txt"), "content");

        FileOperations.deleteRecursively(tree);

        assertFalse(Files.exists(tree));
    }

    @Test
    void testDeleteRecursivelyIgnoresMissingPath() throws IOException {
        FileOperations.deleteRecursively(folder.resolve("missing"));
        assertTrue(Files.isDirectory(folder));
    }
}
