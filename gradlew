#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROPERTIES_FILE="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "ERROR: $PROPERTIES_FILE tidak ditemukan." >&2
  exit 1
fi

# Baca distributionUrl dari gradle-wrapper.properties dan ubah escape Java properties.
DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPERTIES_FILE" | head -n 1 | sed 's/\\:/:/g; s/\\\\/\\/g')"
if [ -z "$DIST_URL" ]; then
  echo "ERROR: distributionUrl tidak ditemukan di $PROPERTIES_FILE." >&2
  exit 1
fi

ZIP_NAME="${DIST_URL##*/}"
DIST_NAME="${ZIP_NAME%.zip}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_USER_HOME/manual-distributions/$DIST_NAME"
GRADLE_BIN="$INSTALL_DIR/$DIST_NAME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$INSTALL_DIR"
  TMP_ZIP="$INSTALL_DIR/$ZIP_NAME.tmp"
  echo "Mengunduh distribusi Gradle: $DIST_URL"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 20 "$DIST_URL" -o "$TMP_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP_ZIP" "$DIST_URL"
  else
    echo "ERROR: curl atau wget diperlukan untuk mengunduh Gradle." >&2
    exit 1
  fi
  mv "$TMP_ZIP" "$INSTALL_DIR/$ZIP_NAME"
  unzip -q -o "$INSTALL_DIR/$ZIP_NAME" -d "$INSTALL_DIR"
fi

if [ ! -x "$GRADLE_BIN" ]; then
  echo "ERROR: Gradle tidak ditemukan setelah ekstraksi: $GRADLE_BIN" >&2
  exit 1
fi

exec "$GRADLE_BIN" "$@"
