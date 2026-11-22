package ch.jdkupdater.ui;

import javafx.application.Application;

/**
 * Starts the JDK Updater.
 *
 * <p>This class does not extend {@link Application}, so the jar also starts from the class path, where the Java
 * launcher would otherwise refuse a JavaFX main class.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the application with the system proxy settings.
     *
     * @param args named parameters, see {@link JdkUpdaterApp}
     */
    public static void main(final String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        Application.launch(JdkUpdaterApp.class, args);
    }
}
