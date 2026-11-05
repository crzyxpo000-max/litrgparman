#!/usr/bin/env bash
# Her aşamanın sonunda geçmesi gereken kontroller.
#   motor testleri + içerik doğrulaması + debug APK + lint
# Kullanım: scripts/check.sh   (Windows'ta Git Bash ile)
set -euo pipefail

cd "$(dirname "$0")/.."

# JAVA_HOME yoksa Android Studio'nun JDK'sını (JBR) bulmaya çalış.
if [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
    for candidate in \
        "/c/Program Files/Android/Android Studio/jbr" \
        "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
        "$HOME/android-studio/jbr" \
        "/opt/android-studio/jbr"; do
        if [ -x "$candidate/bin/java" ] || [ -x "$candidate/bin/java.exe" ]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [ ! -f local.properties ] && [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    echo "Uyarı: local.properties ya da ANDROID_HOME yok; Android SDK bulunamayabilir." >&2
fi

GRADLE="./gradlew"
[ -x "$GRADLE" ] || GRADLE="bash ./gradlew"

$GRADLE --console=plain \
    :engine:test \
    :engine:validateContent \
    :app:assembleDebug \
    :app:lint

echo
echo "check.sh: tüm kontroller geçti."
