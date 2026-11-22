package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdkUpdaterTest {

    private static final String RELEASE_NAME = "jdk-25.0.5+11";
    private static final String OLD_JDK = "jdk-25.0.4+7";
    private static final String CHECKSUM_PATH = "/download/jdk.zip.sha256.txt";

    private TestServer server;
    private JdkDirectory directory;
    private JdkUpdater updater;
    private RecordingListener listener;

    @BeforeEach
    void setUp(@TempDir final Path tempDir) throws IOException {
        byte[] archive = TestData.jdkZip(RELEASE_NAME);
        server = new TestServer();
        server.serve("/v3/info/available_releases", TestData.availableReleases(25, 27));
        server.serve("/v3/assets/latest/25/hotspot", TestData.assets(RELEASE_NAME,
                server.uri("/download/jdk.zip"), server.uri(CHECKSUM_PATH)));
        server.serve("/download/jdk.zip", archive);
        server.serve(CHECKSUM_PATH, TestData.sha256(archive) + "  jdk.zip\n");

        WebClient webClient = new WebClient();
        updater = new JdkUpdater(new AdoptiumClient(webClient, server.uri("/v3/")), webClient);
        directory = new JdkDirectory(tempDir.resolve("jdk"));
        Files.createDirectories(directory.resolve(OLD_JDK).resolve("bin"));
        listener = new RecordingListener();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void testCheckFindsUpdate() throws IOException, InterruptedException {
        UpdateCheck check = updater.check(directory, ReleaseChannel.LTS);

        assertEquals(RELEASE_NAME, check.latest().name());
        assertFalse(check.isUpToDate());
        assertEquals(Optional.of(OLD_JDK), check.replacedJdk());
    }

    @Test
    void testInstallReplacesOldJdkWhenConfirmed() throws IOException, InterruptedException {
        listener.confirm = true;

        Path jdk = updater.install(directory, updater.check(directory, ReleaseChannel.LTS), listener);

        assertEquals(directory.resolve("jdk-25.0.5.11"), jdk);
        assertTrue(Files.isRegularFile(jdk.resolve("bin/java.exe")));
        assertFalse(Files.exists(directory.resolve(OLD_JDK)));
        assertEquals(List.of(directory.resolve(OLD_JDK)), listener.deleteRequests);
        assertEquals(1.0, listener.lastProgress);
        assertEquals(List.of("jdk-25.0.5.11"), directory.installedJdks());
    }

    @Test
    void testInstallKeepsOldJdkWhenNotConfirmed() throws IOException, InterruptedException {
        listener.confirm = false;

        updater.install(directory, updater.check(directory, ReleaseChannel.LTS), listener);

        assertEquals(List.of(OLD_JDK, "jdk-25.0.5.11"), directory.installedJdks());
        assertTrue(listener.messages.contains("Kept " + directory.resolve(OLD_JDK)));
    }

    @Test
    void testInstallWithoutOldJdkAsksNothing() throws IOException, InterruptedException {
        FileOperations.deleteRecursively(directory.resolve(OLD_JDK));

        updater.install(directory, updater.check(directory, ReleaseChannel.LTS), listener);

        assertEquals(List.of(), listener.deleteRequests);
        assertEquals(List.of("jdk-25.0.5.11"), directory.installedJdks());
    }

    @Test
    void testWrongChecksumChangesNothing() throws IOException, InterruptedException {
        server.serve(CHECKSUM_PATH, "0".repeat(64) + "  jdk.zip\n");
        UpdateCheck check = updater.check(directory, ReleaseChannel.LTS);

        assertThrows(IOException.class, () -> updater.install(directory, check, listener));
        assertEquals(List.of(OLD_JDK), directory.installedJdks());
        assertFalse(Files.exists(directory.resolve(".jdk-update-tmp")));
    }

    private static final class RecordingListener implements InstallListener {

        private final List<String> messages = new ArrayList<>();
        private final List<Path> deleteRequests = new ArrayList<>();
        private double lastProgress;
        private boolean confirm;

        @Override
        public void onMessage(final String message) {
            messages.add(message);
        }

        @Override
        public void onProgress(final double progress) {
            lastProgress = progress;
        }

        @Override
        public boolean confirmDelete(final Path jdk) {
            deleteRequests.add(jdk);
            return confirm;
        }
    }
}
