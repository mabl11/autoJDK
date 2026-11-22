package ch.jdkupdater;

import java.nio.file.Path;

/**
 * Receives feedback while a JDK is installed.
 *
 * <p>The methods are called from the thread that runs the installation.</p>
 */
public interface InstallListener {

    /**
     * Receives a message about the current step.
     *
     * @param message the message
     */
    void onMessage(String message);

    /**
     * Receives the download progress.
     *
     * @param progress the progress from 0.0 to 1.0
     */
    void onProgress(double progress);

    /**
     * Decides whether the JDK replaced by the update may be deleted.
     *
     * @param jdk folder of the replaced JDK
     * @return {@code true} to delete the folder
     * @throws InterruptedException if the thread is interrupted while waiting for the decision
     */
    boolean confirmDelete(Path jdk) throws InterruptedException;
}
