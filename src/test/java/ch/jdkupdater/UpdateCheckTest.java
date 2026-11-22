package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateCheckTest {

    private static final Release LATEST = new Release("jdk-25.0.5+11",
            URI.create("https://example.org/jdk.zip"), URI.create("https://example.org/jdk.zip.sha256.txt"));

    @Test
    void testUpToDateWhenFolderExists() {
        assertTrue(new UpdateCheck(LATEST, List.of("jdk-25.0.5.11")).isUpToDate());
    }

    @Test
    void testUpToDateWhenFolderWithPlusExists() {
        assertTrue(new UpdateCheck(LATEST, List.of("jdk-25.0.5+11")).isUpToDate());
    }

    @Test
    void testNotUpToDateWithOlderJdk() {
        assertFalse(new UpdateCheck(LATEST, List.of("jdk-25.0.4+7")).isUpToDate());
    }

    @Test
    void testReplacedJdkIsNewestOlderJdk() {
        UpdateCheck check = new UpdateCheck(LATEST, List.of("jdk-27.0.1", "jdk-21.0.2", "jdk-25.0.4+7"));
        assertEquals(Optional.of("jdk-25.0.4+7"), check.replacedJdk());
    }

    @Test
    void testNoReplacedJdkWhenAllAreNewer() {
        assertEquals(Optional.empty(), new UpdateCheck(LATEST, List.of("jdk-27.0.1")).replacedJdk());
    }

    @Test
    void testNewestInstalled() {
        UpdateCheck check = new UpdateCheck(LATEST, List.of("jdk-27.0.1", "jdk-21.0.2", "jdk-25.0.4+7"));
        assertEquals(Optional.of("jdk-27.0.1"), check.newestInstalled());
    }

    @Test
    void testNewestInstalledIsEmptyWithoutJdks() {
        assertEquals(Optional.empty(), new UpdateCheck(LATEST, List.of()).newestInstalled());
    }
}
