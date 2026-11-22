#!/usr/bin/env bash
# Builds dist/JdkUpdater.zip: JdkUpdater.exe + app/jdk-updater.jar + runtime/ (Java 25 with JavaFX for Windows).
# Runs on Linux or WSL. Run the tests first with: mvn test
#
#   BUILD_JDK  JDK 25 with JavaFX for this machine (e.g. Liberica Full JDK 25 for Linux)
#   WIN_JDK    JDK 25 with JavaFX jmods for Windows (e.g. Liberica Full JDK 25 for Windows)
#   needs:     x86_64-w64-mingw32-gcc (apt install gcc-mingw-w64-x86-64) and zip
set -euo pipefail
cd "$(dirname "$0")"

BUILD_JDK="${BUILD_JDK:?set BUILD_JDK}"
WIN_JDK="${WIN_JDK:?set WIN_JDK}"
OUT=dist/JdkUpdater

rm -rf build dist
mkdir -p build/classes "$OUT/app"

find src/main/java -name '*.java' > build/sources.txt
"$BUILD_JDK/bin/javac" --release 25 --add-modules javafx.controls -encoding UTF-8 -d build/classes @build/sources.txt
"$BUILD_JDK/bin/jar" --create --file "$OUT/app/jdk-updater.jar" --main-class ch.jdkupdater.ui.Launcher \
  -C build/classes .

"$BUILD_JDK/bin/jlink" \
  --module-path "$WIN_JDK/jmods" \
  --add-modules javafx.controls,java.net.http \
  --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
  --output "$OUT/runtime"

x86_64-w64-mingw32-gcc -O2 -municode -mwindows -static -s -o "$OUT/JdkUpdater.exe" launcher/launcher.c

(cd dist && zip -qr9 JdkUpdater.zip JdkUpdater)
echo "Built dist/JdkUpdater.zip"
