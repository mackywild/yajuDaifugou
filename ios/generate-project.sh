#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v xcodegen >/dev/null 2>&1; then
  echo "XcodeGen が必要です: brew install xcodegen"
  exit 1
fi
xcodegen generate
printf '\n生成完了: ios/DaifugoIOS.xcodeproj\n'
