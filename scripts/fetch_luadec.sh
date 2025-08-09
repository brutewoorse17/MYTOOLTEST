#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
VENDOR_DIR="$ROOT_DIR/app/src/main/cpp/third_party"
LUADEC_DIR="$VENDOR_DIR/luadec"

rm -rf "$LUADEC_DIR"
mkdir -p "$VENDOR_DIR"

TMP_ZIP=$(mktemp)
TMP_DIR=$(mktemp -d)

echo "Downloading luadec..."
curl -sSL -o "$TMP_ZIP" https://codeload.github.com/viruscamp/luadec/zip/refs/heads/master
unzip -q "$TMP_ZIP" -d "$TMP_DIR"

mv "$TMP_DIR"/luadec-master "$LUADEC_DIR"

rm -f "$TMP_ZIP"
rm -rf "$TMP_DIR"

echo "Vendored luadec to: $LUADEC_DIR"
echo "Next: update CMakeLists.txt to include and build luadec sources per version (5.1/5.2/5.3)."