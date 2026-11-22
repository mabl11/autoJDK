package ch.jdkupdater;

import java.net.URI;

/**
 * A JDK release that can be downloaded.
 *
 * @param name        release name as published by Adoptium, for example {@code jdk-25.0.5+11}
 * @param downloadUri URI of the zip archive
 * @param checksumUri URI of the file with the SHA-256 checksum of the archive
 */
public record Release(String name, URI downloadUri, URI checksumUri) {

    /**
     * Returns the folder name for the installation, for example {@code jdk-25.0.5.11}.
     *
     * <p>The plus sign is replaced by a dot because some tools cannot handle it in a path.</p>
     *
     * @return the folder name
     */
    public String folderName() {
        return name.replace('+', '.');
    }

    /**
     * Returns the file name of the archive, for example {@code OpenJDK25U-jdk_x64_windows_hotspot_25.0.5_11.zip}.
     *
     * @return the file name of the archive
     */
    public String archiveName() {
        String path = downloadUri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
