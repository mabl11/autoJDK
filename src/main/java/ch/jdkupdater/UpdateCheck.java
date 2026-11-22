package ch.jdkupdater;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Result of comparing the latest release with the locally installed JDKs.
 *
 * @param latest    the latest release
 * @param installed folder names of the installed JDKs
 */
public record UpdateCheck(Release latest, List<String> installed) {

    private static final Comparator<String> VERSION_ORDER = new VersionComparator();

    /**
     * Creates a check result with an unmodifiable copy of the installed JDKs.
     *
     * @param latest    the latest release
     * @param installed folder names of the installed JDKs
     */
    public UpdateCheck {
        installed = List.copyOf(installed);
    }

    /**
     * Tells whether the latest release is already installed.
     *
     * @return {@code true} if a folder of the latest release exists
     */
    public boolean isUpToDate() {
        return installed.contains(latest.folderName()) || installed.contains(latest.name());
    }

    /**
     * Returns the newest installed JDK.
     *
     * @return the folder name of the newest JDK, empty if none is installed
     */
    public Optional<String> newestInstalled() {
        return installed.stream().max(VERSION_ORDER);
    }

    /**
     * Returns the JDK that the latest release replaces: the newest installed JDK that is older than the release.
     *
     * @return the folder name of the replaced JDK, empty if no installed JDK is older
     */
    public Optional<String> replacedJdk() {
        return installed.stream()
                .filter(jdk -> VERSION_ORDER.compare(jdk, latest.folderName()) < 0)
                .max(VERSION_ORDER);
    }
}
