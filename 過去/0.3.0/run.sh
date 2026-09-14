#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${DAIFUGO_PASSWORD:-}" ]]; then
  echo "ERROR: DAIFUGO_PASSWORD を設定してください。" >&2
  echo "例: DAIFUGO_PASSWORD='change-me' ./run.sh" >&2
  exit 1
fi

./gradlew bootRun
