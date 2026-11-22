package ch.jdkupdater;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Changes environment variables of the current Windows user.
 */
public final class UserEnvironment {

    private UserEnvironment() {
    }

    /**
     * Sets a permanent environment variable of the current user with the Windows command {@code setx}.
     *
     * <p>Only programs started afterwards see the new value.</p>
     *
     * @param name  name of the variable, for example {@code JAVA_HOME}
     * @param value the new value
     * @throws IOException          if {@code setx} cannot be started or fails
     * @throws InterruptedException if the thread is interrupted while waiting for {@code setx}
     */
    public static void setVariable(final String name, final String value) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("setx", name, value).redirectErrorStream(true).start();
        process.getInputStream().transferTo(OutputStream.nullOutputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("setx failed with exit code " + exitCode);
        }
    }
}
