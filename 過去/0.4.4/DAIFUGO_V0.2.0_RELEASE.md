# Daifugo v0.2.0 — 野獣ルールアップデート

## Release Highlight
新ローカルルール「野獣ルール」と7渡しを追加。

- 8+10を持つと野獣対象
- 7渡しで隣プレイヤーにも野獣ルールを伝播可能
- 一度野獣対象になったら解除不可
- 最後の8/10は手放せない
- 余分な8/10は通常利用・7渡し可能
- 最後の2枚を8→10で上がると野獣上がり成功
- 野獣上がり違反は反則最下位
- 対象化と成功は全端末共有イベント

## Audio
音声は同梱していません。

Android側で以下を配置後、APKを再ビルドするだけで有効になります。

```text
android/app/src/main/res/raw/yaju_available.mp3
android/app/src/main/res/raw/yaju_success.mp3
```

`.wav`でも同じベース名で使用できます。

## Verification
- Game core Java 21 compile: PASS
- Changed Server/API source type check: PASS
- Existing + new game/room tests: 87 PASS
- Login rate-limit regression tests: 2 PASS
- Total lightweight JUnit-compatible regression: 89 PASS
- YAJU dedicated smoke: PASS
- Web JavaScript syntax: PASS
- Random 4-player simulation x300: deadlock 0 / exception 0

## Build note
この検証環境では `services.gradle.org` のDNS解決ができないため、Gradle本体の取得を伴う `./gradlew test` とAndroid APK assembleは未実行です。
GitHub Actionsでは通常のGradleテスト / APKビルドを行う構成を維持しています。
