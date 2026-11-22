# autoJDK

Small Windows tool with a JavaFX window. It replaces the newest JDK in `C:\jdk` with the latest
[Eclipse Temurin](https://adoptium.net) release and can set `JAVA_HOME`.

## Use

Download `JdkUpdater.zip` from the Releases page, unzip it anywhere (not inside `C:\jdk`) and run `JdkUpdater.exe`.
No Java installation is needed, the zip contains its own Java 25 runtime.

The exe is not officially signed, so Windows shows "Windows protected your PC" the first time:
click **More info → Run anyway**. Use on your own risk. 

What it does:

1. **Check** : looks up the latest LTS or newest release and the newest JDK in the folder.
2. **Update**: downloads the zip, verifies the SHA-256 checksum, installs it as `jdk-25.0.5.11`,
   asks before deleting the JDK it replaces and sets `JAVA_HOME` if ticked.

Optional parameters: `--dir=D:\java` (other folder), `--api=<uri>` (other Adoptium API).

## Develop

Requires JDK 25 and Maven

```bash
mvn test          # unit tests
mvn javafx:run    # start the app
```

| Package | Content |
|---|---|
| `ch.jdkupdater` | logic without JavaFX: `JdkUpdater`, `AdoptiumClient`, `WebClient`, `JdkDirectory`, ... |
| `ch.jdkupdater.ui` | JavaFX: `Launcher` (main class), `JdkUpdaterApp`, `MainWindow` |

## Build the Windows zip

Runs on Linux or WSL, see the comments in `package-windows.sh`:

```bash
BUILD_JDK=/path/to/linux-jdk-25-full WIN_JDK=/path/to/windows-jdk-25-full ./package-windows.sh
```

Result: `dist/JdkUpdater.zip` with `JdkUpdater.exe` (`launcher/launcher.c`), `app/jdk-updater.jar` and a
Java 25 runtime with JavaFX made with `jlink`.
