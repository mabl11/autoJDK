package ch.jdkupdater.ui;

import ch.jdkupdater.InstallListener;
import ch.jdkupdater.JdkDirectory;
import ch.jdkupdater.JdkUpdater;
import ch.jdkupdater.ReleaseChannel;
import ch.jdkupdater.UpdateCheck;
import ch.jdkupdater.UserEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Main window: shows the latest and the installed JDK and starts the update.
 *
 * <p>Checks and installations run on a background thread. As {@link InstallListener} the window shows their
 * messages and progress and asks before the old JDK is deleted.</p>
 */
public final class MainWindow implements InstallListener {

    private static final String TITLE = "JDK Updater";
    private static final String JAVA_HOME = "JAVA_HOME";
    private static final double WIDTH = 580;
    private static final double HEIGHT = 420;
    private static final double GAP = 10;
    private static final double SMALL_GAP = 6;

    private final JdkUpdater updater;
    private final TextField directoryField;
    private final RadioButton ltsButton = new RadioButton("LTS");
    private final RadioButton newestButton = new RadioButton("Newest");
    private final CheckBox javaHomeBox = new CheckBox("Set " + JAVA_HOME);
    private final Label latestLabel = new Label();
    private final Label installedLabel = new Label();
    private final Button checkButton = new Button("Check");
    private final Button updateButton = new Button("Update");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TextArea logArea = new TextArea();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<UpdateCheck> availableUpdate = new SimpleObjectProperty<>();

    /**
     * Creates the window.
     *
     * @param updater   checks for and installs updates
     * @param directory folder with the JDKs shown at the start
     */
    public MainWindow(final JdkUpdater updater, final JdkDirectory directory) {
        this.updater = updater;
        this.directoryField = new TextField(directory.path().toString());
    }

    /**
     * Shows the window on a stage and starts the first check.
     *
     * @param stage the stage
     */
    public void show(final Stage stage) {
        stage.setTitle(TITLE);
        stage.setScene(new Scene(createContent(stage), WIDTH, HEIGHT));
        stage.show();
        startCheck();
    }

    @Override
    public void onMessage(final String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    @Override
    public void onProgress(final double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

    @Override
    public boolean confirmDelete(final Path jdk) throws InterruptedException {
        FutureTask<Boolean> question = new FutureTask<>(() -> askToDelete(jdk));
        Platform.runLater(question);
        try {
            return question.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("The confirmation dialog failed", e);
        }
    }

    private Parent createContent(final Stage stage) {
        configureControls();
        ColumnConstraints growing = new ColumnConstraints();
        growing.setHgrow(Priority.ALWAYS);

        GridPane form = new GridPane(GAP, GAP);
        form.getColumnConstraints().addAll(new ColumnConstraints(), growing);
        form.addColumn(0, new Label("Folder:"), new Label("Release:"), new Label("Latest:"), new Label("Installed:"));
        form.addColumn(1, directoryRow(stage), new HBox(GAP, ltsButton, newestButton, javaHomeBox), latestLabel,
                installedLabel);

        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox root = new VBox(GAP, form, new HBox(SMALL_GAP, checkButton, updateButton), progressBar, logArea);
        root.setPadding(new Insets(GAP));
        return root;
    }

    private void configureControls() {
        ToggleGroup channels = new ToggleGroup();
        ltsButton.setToggleGroup(channels);
        newestButton.setToggleGroup(channels);
        ltsButton.setSelected(true);
        javaHomeBox.setSelected(true);
        javaHomeBox.setMnemonicParsing(false);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        logArea.setEditable(false);
        logArea.setWrapText(true);

        checkButton.setOnAction(event -> startCheck());
        updateButton.setOnAction(event -> startInstall());
        checkButton.disableProperty().bind(busy);
        updateButton.disableProperty().bind(busy.or(availableUpdate.isNull()));
        channels.selectedToggleProperty().addListener((observable, oldValue, newValue) -> availableUpdate.set(null));
        directoryField.textProperty().addListener((observable, oldValue, newValue) -> availableUpdate.set(null));
    }

    private HBox directoryRow(final Stage stage) {
        Button browseButton = new Button("...");
        browseButton.setOnAction(event -> chooseDirectory(stage));
        HBox.setHgrow(directoryField, Priority.ALWAYS);
        return new HBox(SMALL_GAP, directoryField, browseButton);
    }

    private void chooseDirectory(final Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        File current = new File(directoryField.getText());
        if (current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }
        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            directoryField.setText(chosen.getPath());
        }
    }

    private void startCheck() {
        JdkDirectory directory = selectedDirectory();
        ReleaseChannel channel = selectedChannel();
        availableUpdate.set(null);
        onMessage("Looking up the latest release (" + channel + ")");

        Task<UpdateCheck> task = new Task<>() {
            @Override
            protected UpdateCheck call() throws IOException, InterruptedException {
                return updater.check(directory, channel);
            }
        };
        task.setOnSucceeded(event -> showCheck(task.getValue()));
        runInBackground(task);
    }

    private void showCheck(final UpdateCheck check) {
        latestLabel.setText(check.latest().name());
        installedLabel.setText(check.newestInstalled().orElse("none"));
        if (check.isUpToDate()) {
            onMessage("Up to date: " + check.latest().folderName());
            return;
        }
        availableUpdate.set(check);
        String replaces = check.replacedJdk().map(jdk -> ", replaces " + jdk).orElse("");
        onMessage("Update available: " + check.latest().name() + replaces);
    }

    private void startInstall() {
        JdkDirectory directory = selectedDirectory();
        UpdateCheck check = availableUpdate.get();
        boolean updateJavaHome = javaHomeBox.isSelected();
        progressBar.setProgress(0);

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws IOException, InterruptedException {
                Path jdk = updater.install(directory, check, MainWindow.this);
                if (updateJavaHome) {
                    setJavaHome(jdk);
                }
                return jdk;
            }
        };
        task.setOnSucceeded(event -> startCheck());
        runInBackground(task);
    }

    private void setJavaHome(final Path jdk) throws InterruptedException {
        try {
            UserEnvironment.setVariable(JAVA_HOME, jdk.toString());
            onMessage(JAVA_HOME + " = " + jdk + " (for programs started from now on)");
        } catch (IOException e) {
            onMessage("Could not set " + JAVA_HOME + ": " + e.getMessage());
        }
    }

    private void runInBackground(final Task<?> task) {
        task.setOnFailed(event -> onMessage("ERROR: " + describe(task.getException())));
        busy.bind(task.runningProperty());
        Thread.ofVirtual().name("jdk-updater").start(task);
    }

    private boolean askToDelete(final Path jdk) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + jdk + "?", ButtonType.YES, ButtonType.NO);
        alert.setTitle(TITLE);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private JdkDirectory selectedDirectory() {
        return new JdkDirectory(Path.of(directoryField.getText().strip()));
    }

    private ReleaseChannel selectedChannel() {
        if (newestButton.isSelected()) {
            return ReleaseChannel.NEWEST;
        }
        return ReleaseChannel.LTS;
    }

    private static String describe(final Throwable error) {
        return Objects.requireNonNullElse(error.getMessage(), error.toString());
    }
}
