package ch.jdkupdater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * File operations that the Java class library does not offer as single methods.
 */
public final class FileOperations {

    private static final String SHA_256 = "SHA-256";

    private FileOperations() {
    }

    /**
     * Returns the SHA-256 checksum of a file.
     *
     * @param file the file
     * @return the checksum as lowercase hex string
     * @throws IOException if the file cannot be read
     */
    public static String sha256(final Path file) throws IOException {
        MessageDigest digest = sha256Digest();
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Extracts a zip archive into a folder.
     *
     * @param archive      the zip archive
     * @param targetFolder the folder to extract into, created if missing
     * @throws IOException if the archive cannot be read, a file cannot be written or an entry would end up
     *                     outside the target folder
     */
    public static void unzip(final Path archive, final Path targetFolder) throws IOException {
        Path root = targetFolder.toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                extract(zip, entry, root);
                entry = zip.getNextEntry();
            }
        }
    }

    /**
     * Deletes a file or a folder with all its content. Nothing happens if the path does not exist.
     *
     * @param path the file or folder
     * @throws IOException if something cannot be deleted
     */
    public static void deleteRecursively(final Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        List<Path> deepestFirst;
        try (Stream<Path> paths = Files.walk(path)) {
            deepestFirst = paths.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path current : deepestFirst) {
            Files.delete(current);
        }
    }

    private static void extract(final ZipInputStream zip, final ZipEntry entry, final Path root) throws IOException {
        Path target = root.resolve(entry.getName()).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Zip entry outside the target folder: " + entry.getName());
        }
        if (entry.isDirectory()) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(zip, target);
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime must support " + SHA_256, e);
        }
    }
}
