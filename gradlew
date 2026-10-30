#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROPERTIES_FILE="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "ERROR: $PROPERTIES_FILE tidak ditemukan." >&2
  exit 1
fi

# distributionUrl pada Java properties meng-escape titik dua sebagai \:.
# Cukup ubah \: menjadi :; tidak perlu manipulasi backslash lain.
DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPERTIES_FILE" | head -n 1 | sed 's/\\:/:/g')"

if [ -z "$DIST_URL" ]; then
  echo "ERROR: distributionUrl tidak ditemukan di $PROPERTIES_FILE." >&2
  exit 1
fi

ZIP_NAME="${DIST_URL##*/}"
ARCHIVE_NAME="${ZIP_NAME%.zip}"
DIST_NAME="$(printf '%s' "$ARCHIVE_NAME" | sed -E 's/-(bin|all|src)$//')"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
CACHE_DIR="$GRADLE_USER_HOME/manual-distributions"
INSTALL_DIR="$CACHE_DIR/$DIST_NAME"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"
ZIP_PATH="$CACHE_DIR/$ZIP_NAME"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$CACHE_DIR"
  if [ ! -f "$ZIP_PATH" ]; then
    TMP_ZIP="$ZIP_PATH.tmp"
    echo "Mengunduh distribusi Gradle: $DIST_URL"
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --connect-timeout 20 "$DIST_URL" -o "$TMP_ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$TMP_ZIP" "$DIST_URL"
    else
      echo "ERROR: curl atau wget diperlukan." >&2
      exit 1
    fi
    mv "$TMP_ZIP" "$ZIP_PATH"
  fi

  echo "Mengekstrak Gradle ke: $CACHE_DIR"
  unzip -q -o "$ZIP_PATH" -d "$CACHE_DIR"
fi

if [ ! -x "$GRADLE_BIN" ]; then
  echo "ERROR: Gradle tidak ditemukan setelah ekstraksi: $GRADLE_BIN" >&2
  ls -la "$CACHE_DIR" >&2 || true
  exit 1
fi

exec "$GRADLE_BIN" "$@"
