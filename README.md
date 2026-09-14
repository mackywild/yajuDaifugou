# Daifugo v0.4.0 — CPU BATTLE UPDATE

Java 21 + Spring Bootをゲームサーバーの正本とし、PCブラウザ / Android / iPhoneでクロスプレイできるオンライン大富豪です。

v0.4.0では、従来の部屋対戦を **「マルチプレイ」** としてメインメニュー配下へ整理し、1人で遊べるCPU戦 **【ひとりでイク】** を追加しました。

## ゲームモード

### 🌐 マルチプレイ
- PCブラウザ / Android / iPhoneクロスプレイ
- 2〜4人
- 部屋作成 / 8文字ルームID参加
- ホストによるゲーム開始

### 🤖 CPU戦【ひとりでイク】
- 人間1人 + CPU 1〜3人
- 作成直後にゲーム開始
- CPUの手番はサーバー側で自動進行
- 難易度: **簡単 / 普通 / 難しい / N-GOD**
- ジョーカー枚数・特殊ルールを対戦ごとに設定可能

CPU戦の詳細仕様は [`CPU_BATTLE_SPEC.md`](CPU_BATTLE_SPEC.md) を参照してください。

## CPU難易度

| 難易度 | 実装 |
|---|---|
| 簡単 | 合法手からほぼランダム |
| 普通 | 手札削減・強札温存・上がりを考慮。一定確率でミス |
| 難しい | 相手残枚数、手札構造、8切り、7渡し等を考慮するヒューリスティック |
| N-GOD | 自己対戦で学習した38特徴量MLP + 難しいCPUの安全項 |

N-GODは5,000ゲーム・278,188 state-actionサンプルで学習済みです。学習手順と再生成方法は [`tools/ml/README.md`](tools/ml/README.md) を参照してください。

### N-GODベンチマーク

各CPUを「難しいCPU × 3」と4,000ゲームずつ対戦させた検証結果:

```text
簡単     1位率  5.3%   平均順位 3.309
普通     1位率 23.0%   平均順位 2.503
難しい   1位率 25.9%   平均順位 2.485
N-GOD    1位率 31.3%   平均順位 2.173
```

全難易度でdeadlock=0。1位率・平均順位とも **簡単 < 普通 < 難しい < N-GOD** の順序を確認しています。

## CPU戦で設定できるルール

- CPU人数: 1〜3人
- ジョーカー: 0〜2枚
- 革命
- 8切り
- マーク縛り
- 7渡し
- 野獣ルール
- 禁止上がり

野獣ルールON時は成立条件のため8切りも必須として、Web / Android / iOS / サーバーすべてで同じ制約を適用します。

## 共通ゲーム機能

- 2〜4人対戦
- シングル / ペア / 3枚 / 4枚 / 階段
- 革命
- 8切り
- 7渡し
- マーク縛り
- ジョーカー単体
- スペード3返し
- 2 / 8 / ジョーカー / スペード3の禁止上がり → 反則最下位
- 野獣ルール
  - 8+10所持で対象化
  - 7渡しによる対象伝播
  - 8/10保持義務
  - 最後の2枚を8→10で野獣上がり
  - 違反上がりは反則最下位
  - 全端末共有音声イベント
- 順位判定

## セキュリティ / 通信

- `DAIFUGO_PASSWORD` 必須
- HTTPセッションによる本人紐付け
- 他プレイヤーの手札秘匿
- CSRF保護
- ログイン試行回数制限
- 無操作ルームTTL
- RESTをゲーム操作の正本に使用
- WebSocketは `STATE_CHANGED` 通知専用

## サーバー起動

Java 21を使用してください。

### macOS / Linux

```bash
export DAIFUGO_PASSWORD='十分に長いパスワード'
./gradlew bootRun
```

### Windows CMD

```bat
set DAIFUGO_PASSWORD=十分に長いパスワード
gradlew.bat bootRun
```

既定ポートは8080です。

## Android

詳細: [`android/README.md`](android/README.md)

実機からはサーバーPCのLAN内IPを指定します。

```text
http://192.168.1.20:8080
```

Android Emulatorでは通常:

```text
http://10.0.2.2:8080
```

## iPhone / iOS

詳細: [`ios/README.md`](ios/README.md)

```bash
brew install xcodegen
cd ios
./generate-project.sh
open DaifugoIOS.xcodeproj
```

## GitHub Actions

- `Server Test`: Java 21 / Spring Boot / JUnit
- `Android APK`: v0.4.0 debug APK
- `iOS Build`: XcodeGen + iOS Simulator build

## 野獣ルール音声

音声ファイル自体は同梱していません。

Android:

```text
android/app/src/main/res/raw/yaju_available.mp3
android/app/src/main/res/raw/yaju_success.mp3
```

iOS:

```text
ios/DaifugoIOS/AudioResources/yaju_available.mp3
ios/DaifugoIOS/AudioResources/yaju_success.mp3
```

詳細: [`YAJU_RULE_SPEC.md`](YAJU_RULE_SPEC.md)

## v0.4.0検証

- Java 21ゲームコア + CPU実装: compile PASS
- CPU機能スモーク: PASS
- Web JavaScript構文: PASS
- Swift Models / REST / WebSocket層: typecheck PASS
- ランダム設定自己対戦300ゲーム: deadlock 0（16,840 state-action）
- 難易度ベンチマーク: 各4,000ゲーム、全難易度deadlock 0
- 全ゲーム/CPUテストソース: Java 21型・構文チェック PASS

この実行環境ではGradle distributionを外部取得できないため、最終 `./gradlew test` / Android APK / Xcode buildはGitHub Actionsまたはローカル開発環境で実行してください。
