package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdoptiumClientTest {

    private TestServer server;
    private AdoptiumClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new TestServer();
        server.serve("/v3/info/available_releases", TestData.availableReleases(25, 27));
        client = new AdoptiumClient(new WebClient(), server.uri("/v3/"));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void testFindLatestLts() throws IOException, InterruptedException {
        serveAssets(25, "jdk-25.0.5+11");

        Release release = client.findLatest(ReleaseChannel.LTS);

        assertEquals("jdk-25.0.5+11", release.name());
        assertEquals(server.uri("/download/jdk.zip"), release.downloadUri());
        assertEquals(server.uri("/download/jdk.zip.sha256.txt"), release.checksumUri());
    }

    @Test
    void testFindLatestNewest() throws IOException, InterruptedException {
        serveAssets(27, "jdk-27.0.1+9");
        assertEquals("jdk-27.0.1+9", client.findLatest(ReleaseChannel.NEWEST).name());
    }

    @Test
    void testNewestFallsBackToPreviousVersionWithoutBuild() throws IOException, InterruptedException {
        serveAssets(26, "jdk-26.0.2+8");
        assertEquals("jdk-26.0.2+8", client.findLatest(ReleaseChannel.NEWEST).name());
    }

    @Test
    void testEmptyAnswerCountsAsNoBuild() throws IOException, InterruptedException {
        server.serve("/v3/assets/latest/27/hotspot", "[]");
        serveAssets(26, "jdk-26.0.2+8");
        assertEquals("jdk-26.0.2+8", client.findLatest(ReleaseChannel.NEWEST).name());
    }

    @Test
    void testLtsWithoutBuildFails() {
        assertThrows(IOException.class, () -> client.findLatest(ReleaseChannel.LTS));
    }

    @Test
    void testUnexpectedAnswerFails() {
        server.serve("/v3/info/available_releases", "{}");
        assertThrows(IOException.class, () -> client.findLatest(ReleaseChannel.LTS));
    }

    private void serveAssets(final int version, final String releaseName) {
        server.serve("/v3/assets/latest/" + version + "/hotspot", TestData.assets(releaseName,
                server.uri("/download/jdk.zip"), server.uri("/download/jdk.zip.sha256.txt")));
    }
}
