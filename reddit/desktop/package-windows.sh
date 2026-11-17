#!/usr/bin/env bash
# Builds reddit/desktop/build/UpvoteSaver-windows.zip: the app jar, a Windows Java runtime and a launcher.
# Runs on any OS (the runtime is downloaded, not built). Usage: reddit/desktop/package-windows.sh
set -euo pipefail
cd "$(dirname "$0")/../.."

GRADLE="./gradlew"; [ -x "$GRADLE" ] || GRADLE="bash ./gradlew"
$GRADLE --console=plain -q :reddit:desktop:fatJar

OUT=reddit/desktop/build/windows
CACHE=reddit/desktop/build/jre-cache
rm -rf "$OUT"; mkdir -p "$OUT/UpvoteSaver" "$CACHE"

# Eclipse Temurin 21 JRE for Windows x64.
if [ ! -f "$CACHE/jre.zip" ]; then
    curl -fsSL -o "$CACHE/jre.zip" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
fi
rm -rf "$CACHE/unpacked"; mkdir -p "$CACHE/unpacked"
unzip -q "$CACHE/jre.zip" -d "$CACHE/unpacked"
mv "$CACHE/unpacked"/*/ "$OUT/UpvoteSaver/jre"

cp reddit/desktop/build/libs/upvote-saver.jar "$OUT/UpvoteSaver/"
printf '@echo off\r\nstart "" "%%~dp0jre\\bin\\javaw.exe" -jar "%%~dp0upvote-saver.jar" %%*\r\n' > "$OUT/UpvoteSaver/UpvoteSaver.bat"
cp reddit/desktop/README-windows.txt "$OUT/UpvoteSaver/README.txt"

rm -f reddit/desktop/build/UpvoteSaver-windows.zip
(cd "$OUT" && zip -qr ../UpvoteSaver-windows.zip UpvoteSaver)
ls -la reddit/desktop/build/UpvoteSaver-windows.zip
