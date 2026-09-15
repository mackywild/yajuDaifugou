# 野獣ルール音声の入れ方

Daifugo v0.2.0では音声ファイル自体を同梱していません。
友人に録音してもらった音声を配置するだけで、コード変更なしで有効になります。

## 配置先

```text
android/app/src/main/res/raw/
```

フォルダがなければ作成してください。

## 必須ファイル名

### 野獣対象化

```text
yaju_available.mp3
```

または

```text
yaju_available.wav
```

想定音声: 「野獣上がりできます」

### 野獣上がり成功

```text
yaju_success.mp3
```

または

```text
yaju_success.wav
```

想定音声: 「やりますねぇ」

## 動作

- ファイルが存在する → 全端末で対象イベント発生時に再生
- ファイルが存在しない → 無音でゲーム続行
- 同じGameStateをポーリングで再取得してもイベントIDで重複再生しない

配置後はAPKを再ビルドしてください。

```bash
cd android
./gradlew :app:assembleDebug
```
