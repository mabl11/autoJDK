package ch.jdkupdater;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Checks for a newer JDK and installs it in place of the old one.
 */
public final class JdkUpdater {

    private static final String WORK_FOLDER = ".jdk-update-tmp";
    private static final String ARCHIVE_FILE = "jdk.zip";
    private static final String EXTRACT_FOLDER = "extracted";
    private static final String JAVA_EXECUTABLE = "bin/java.exe";

    private final AdoptiumClient adoptiumClient;
    private final WebClient webClient;

    /**
     * Creates an updater.
     *
     * @param adoptiumClient finds the latest release
     * @param webClient      downloads the release
     */
    public JdkUpdater(final AdoptiumClient adoptiumClient, final WebClient webClient) {
        this.adoptiumClient = adoptiumClient;
        this.webClient = webClient;
    }

    /**
     * Compares the latest release of a channel with the JDKs in a folder.
     *
     * @param directory folder with the installed JDKs
     * @param channel   the release channel
     * @return the result of the comparison
     * @throws IOException          if the API or the folder cannot be read
     * @throws InterruptedException if the thread is interrupted while waiting for the API
     */
    public UpdateCheck check(final JdkDirectory directory, final ReleaseChannel channel)
            throws IOException, InterruptedException {
        return new UpdateCheck(adoptiumClient.findLatest(channel), directory.installedJdks());
    }

    /**
     * Installs the latest release of a check and then deletes the JDK it replaces.
     *
     * <p>The archive is verified with its SHA-256 checksum before anything in the folder is changed. The replaced
     * JDK is only deleted if the listener confirms it. If it cannot be deleted, for example because a program
     * still uses it, the listener is told and the new JDK stays installed.</p>
     *
     * @param directory folder with the installed JDKs
     * @param check     result of {@link #check(JdkDirectory, ReleaseChannel)}, not up to date
     * @param listener  receives messages and progress and confirms the deletion
     * @return the folder of the new JDK
     * @throws IOException          if the download, the checksum or the installation fails
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public Path install(final JdkDirectory directory, final UpdateCheck check, final InstallListener listener)
            throws IOException, InterruptedException {
        Path workFolder = directory.resolve(WORK_FOLDER);
        Path target = directory.resolve(check.latest().folderName());
        FileOperations.deleteRecursively(workFolder);
        Files.createDirectories(workFolder);
        try {
            Files.move(downloadAndExtract(check.latest(), workFolder, listener), target);
        } finally {
            FileOperations.deleteRecursively(workFolder);
        }
        listener.onMessage("Installed " + target);

        Optional<String> replacedJdk = check.replacedJdk();
        if (replacedJdk.isPresent()) {
            deleteIfConfirmed(directory.resolve(replacedJdk.get()), listener);
        }
        return target;
    }

    private Path downloadAndExtract(final Release release, final Path workFolder, final InstallListener listener)
            throws IOException, InterruptedException {
        Path archive = workFolder.resolve(ARCHIVE_FILE);
        listener.onMessage("Downloading " + release.archiveName());
        webClient.download(release.downloadUri(), archive, listener::onProgress);
        verifyChecksum(release, archive);

        listener.onMessage("Checksum OK, extracting");
        Path extractFolder = workFolder.resolve(EXTRACT_FOLDER);
        FileOperations.unzip(archive, extractFolder);
        return findJdk(extractFolder);
    }

    private void verifyChecksum(final Release release, final Path archive) throws IOException, InterruptedException {
        String checksumFile = webClient.fetchText(release.checksumUri()).strip();
        String expected = checksumFile.split("\\s+")[0];
        if (!expected.equalsIgnoreCase(FileOperations.sha256(archive))) {
            throw new IOException("Checksum of " + release.archiveName() + " is wrong, nothing was changed");
        }
    }

    private static Path findJdk(final Path extractFolder) throws IOException {
        try (Stream<Path> entries = Files.list(extractFolder)) {
            return entries.filter(entry -> Files.exists(entry.resolve(JAVA_EXECUTABLE)))
                    .findFirst()
                    .orElseThrow(() -> new IOException("The archive contains no JDK"));
        }
    }

    private static void deleteIfConfirmed(final Path oldJdk, final InstallListener listener)
            throws InterruptedException {
        if (!listener.confirmDelete(oldJdk)) {
            listener.onMessage("Kept " + oldJdk);
            return;
        }
        try {
            FileOperations.deleteRecursively(oldJdk);
            listener.onMessage("Deleted " + oldJdk);
        } catch (IOException e) {
            listener.onMessage("Could not delete " + oldJdk + ", is a Java program still running? " + e.getMessage());
        }
    }
}
