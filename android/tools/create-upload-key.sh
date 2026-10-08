#!/usr/bin/env bash
set -euo pipefail

ALIAS="${1:-yaju-upload}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SIGNING_DIR="$ROOT_DIR/signing"
KEYSTORE="$SIGNING_DIR/yaju-upload.jks"
CERTIFICATE="$SIGNING_DIR/yaju-upload-certificate.pem"
BASE64_FILE="$SIGNING_DIR/yaju-upload.jks.b64"

mkdir -p "$SIGNING_DIR"

if [[ -e "$KEYSTORE" ]]; then
  echo "Keystore already exists: $KEYSTORE" >&2
  echo "Delete it only if you intentionally want a NEW upload key." >&2
  exit 1
fi

echo "Creating Google Play upload key..."
echo "You will be prompted for passwords and certificate owner fields."
keytool -genkeypair -v -keystore "$KEYSTORE" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000

echo
echo "Exporting PUBLIC certificate..."
keytool -export -rfc -keystore "$KEYSTORE" -alias "$ALIAS" -file "$CERTIFICATE"

echo
echo "Certificate fingerprints:"
keytool -list -v -keystore "$KEYSTORE" -alias "$ALIAS" | grep -E "SHA1:|SHA256:" || true

if base64 --help 2>&1 | grep -q -- "-w"; then
  base64 -w 0 "$KEYSTORE" > "$BASE64_FILE"
else
  base64 "$KEYSTORE" | tr -d "\n" > "$BASE64_FILE"
fi

echo
echo "Created:"
echo "  Private upload key : $KEYSTORE"
echo "  Public certificate : $CERTIFICATE"
echo "  GitHub Secret value: $BASE64_FILE"
echo
echo "Back up the .jks file and passwords somewhere outside this repository."
echo "GitHub Secrets:"
echo "  YAJU_UPLOAD_KEYSTORE_B64 = contents of yaju-upload.jks.b64"
echo "  YAJU_STORE_PASSWORD       = keystore password"
echo "  YAJU_KEY_ALIAS            = $ALIAS"
echo "  YAJU_KEY_PASSWORD         = key password"
