package ch.jdkupdater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class TestData {

    private TestData() {
    }

    static String availableReleases(final int lts, final int newest) {
        return """
                {
                    "available_lts_releases": [8, 11, 17, 21, %d],
                    "most_recent_feature_release": %d,
                    "most_recent_lts": %d
                }
                """.formatted(lts, newest, lts);
    }

    static String assets(final String releaseName, final URI archive, final URI checksum) {
        return """
                [
                    {
                        "binary": {
                            "installer": {
                                "checksum_link": "https://example.org/jdk.msi.sha256.txt",
                                "link": "https://example.org/jdk.msi"
                            },
                            "package": {
                                "checksum_link": "%s",
                                "link": "%s"
                            }
                        },
                        "release_name": "%s"
                    }
                ]
                """.formatted(checksum, archive, releaseName);
    }

    static byte[] jdkZip(final String rootFolder) throws IOException {
        return zip(rootFolder + "/", rootFolder + "/bin/java.exe", rootFolder + "/release");
    }

    static byte[] zip(final String... entryNames) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : entryNames) {
                zip.putNextEntry(new ZipEntry(name));
                if (!name.endsWith("/")) {
                    zip.write(name.getBytes(StandardCharsets.UTF_8));
                }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static String sha256(final byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
