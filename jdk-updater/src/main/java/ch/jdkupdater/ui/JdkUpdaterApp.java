package ch.jdkupdater.ui;

import ch.jdkupdater.AdoptiumClient;
import ch.jdkupdater.JdkDirectory;
import ch.jdkupdater.JdkUpdater;
import ch.jdkupdater.WebClient;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application that creates the components and shows the main window.
 *
 * <p>Optional named parameters:</p>
 * <ul>
 *   <li>{@code --dir=<folder>} folder with the JDKs, default {@code C:\jdk}</li>
 *   <li>{@code --api=<uri>} base URI of the Adoptium API, default {@link AdoptiumClient#DEFAULT_API}</li>
 * </ul>
 */
public final class JdkUpdaterApp extends Application {

    private static final String DIRECTORY_PARAMETER = "dir";
    private static final String API_PARAMETER = "api";
    private static final String DEFAULT_DIRECTORY = "C:\\jdk";

    /**
     * Creates the application. JavaFX calls this constructor in {@link Application#launch(Class, String...)}.
     */
    public JdkUpdaterApp() {
    }

    @Override
    public void start(final Stage stage) {
        Map<String, String> parameters = getParameters().getNamed();
        Path directory = Path.of(parameters.getOrDefault(DIRECTORY_PARAMETER, DEFAULT_DIRECTORY));
        URI api = URI.create(parameters.getOrDefault(API_PARAMETER, AdoptiumClient.DEFAULT_API.toString()));

        WebClient webClient = new WebClient();
        JdkUpdater updater = new JdkUpdater(new AdoptiumClient(webClient, api), webClient);
        new MainWindow(updater, new JdkDirectory(directory)).show(stage);
    }
}
