# Google Play ストアテスト

## アプリID

Google Play上のパッケージ名（Android applicationId）は次で固定します。

```text
jp.YajuDaifugou.app
```

コード側のKotlin namespaceは既存の `com.example.daifugo.android` を維持しています。
Google Playがアプリ識別に使用するのは `applicationId` です。

> 最初のAABをPlay Consoleへアップロードした後は、このapplicationIdを変更しないでください。

## テスト版バージョン

```text
versionCode = 9
versionName = 0.5.0-test01
```

次のPlay Consoleアップロードでは必ずversionCodeを10以上へ増やします。

## まず内部テスト

Play Consoleでは最初に内部テストを推奨します。

1. Play Consoleでアプリ「野獣大富豪」を作成
2. パッケージ名が `jp.YajuDaifugou.app` のAABをアップロード
3. 内部テスターを登録
4. テスター用リンクからGoogle Play経由でインストール
5. 起動、Play Games認証、CPU戦、オンライン戦、画面遷移を確認

## AAB

通常CIではdebug APKに加えてdebug AABも生成します。
これはパッケージ構成確認や内部アプリ共有向けです。

Play Consoleのテストトラックへ継続配信する署名済みrelease AABは、
GitHub Actionsの `Android Store Test AAB` を手動実行して生成します。

### 必要なGitHub Actions Secrets

- `YAJU_UPLOAD_KEYSTORE_B64`
- `YAJU_STORE_PASSWORD`
- `YAJU_KEY_ALIAS`
- `YAJU_KEY_PASSWORD`

`YAJU_UPLOAD_KEYSTORE_B64` はアップロード用keystoreをBase64化した値です。

アップロード鍵は更新版の配信に関わるため、リポジトリへkeystoreやパスワードをコミットしないでください。

## Google Play Games Services

Play Games認証をストアテストする場合、Play Console / Google Cloud側のAndroid OAuth設定も次の組み合わせへ合わせます。

- Package name: `jp.YajuDaifugou.app`
- SHA certificate fingerprint: Play App Signing / 使用するテスト証明書のSHA-1

現状 `game_services_project_id` が `0` の開発設定なら、アプリはゲストモードで起動します。
PGSまで試験する段階で実際のGame services project IDへ差し替えます。
