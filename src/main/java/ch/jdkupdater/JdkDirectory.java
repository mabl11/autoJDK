package ch.jdkupdater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * The folder that contains the installed JDKs, for example {@code C:\jdk}.
 *
 * <p>Every subfolder whose name starts with {@code jdk-} counts as an installed JDK.</p>
 *
 * @param path the folder
 */
public record JdkDirectory(Path path) {

    private static final String JDK_PREFIX = "jdk-";

    /**
     * Returns the folder names of the installed JDKs, sorted from oldest to newest.
     *
     * @return the folder names, empty if the folder does not exist
     * @throws IOException if the folder cannot be read
     */
    public List<String> installedJdks() throws IOException {
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(path)) {
            return entries.filter(Files::isDirectory)
                    .map(entry -> entry.getFileName().toString())
                    .filter(name -> name.startsWith(JDK_PREFIX))
                    .sorted(new VersionComparator())
                    .toList();
        }
    }

    /**
     * Returns the path of an entry in this folder.
     *
     * @param name name of the entry
     * @return the path of the entry
     */
    public Path resolve(final String name) {
        return path.resolve(name);
    }
}
