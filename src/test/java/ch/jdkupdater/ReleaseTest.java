package ch.jdkupdater;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ReleaseTest {

    private static final String ARCHIVE_URI =
            "https://example.org/jdk-25.0.5%2B11/OpenJDK25U-jdk_x64_windows_hotspot_25.0.5_11.zip";

    private final Release release = new Release("jdk-25.0.5+11",
            URI.create(ARCHIVE_URI), URI.create(ARCHIVE_URI + ".sha256.txt"));

    @Test
    void testFolderNameReplacesPlus() {
        assertEquals("jdk-25.0.5.11", release.folderName());
    }

    @Test
    void testArchiveNameIsLastPathSegment() {
        assertEquals("OpenJDK25U-jdk_x64_windows_hotspot_25.0.5_11.zip", release.archiveName());
    }
}
