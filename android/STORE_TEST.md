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

## 署名鍵セットアップ

Google Playの新規アプリでは **Play App Signing** を使用し、Googleにアプリ署名鍵を管理してもらいます。
開発者側では別の **upload key（アップロード鍵）** を保持し、その鍵でAABへ署名してPlay Consoleへ送ります。

このリポジトリでは秘密鍵をGitへ保存しません。

### 1. アップロード鍵をローカルで作成

Windows PowerShell:

```powershell
cd android
powershell -ExecutionPolicy Bypass -File .\tools\create-upload-key.ps1
```

macOS / Linux:

```bash
cd android
chmod +x tools/create-upload-key.sh
./tools/create-upload-key.sh
```

生成先:

```text
android/signing/yaju-upload.jks
android/signing/yaju-upload-certificate.pem
android/signing/yaju-upload.jks.b64
```

- `.jks`: **秘密鍵。絶対に公開しない**
- `.pem`: 公開証明書。SHA確認やアップロード鍵リセット時に利用
- `.b64`: GitHub Secret登録用。一時ファイルだが秘密情報として扱う

JKS形式 / RSA 4096-bit / validity 10000 days で生成します。鍵パスワードはkeystoreパスワードと同じでも別でも構いません。

### 2. バックアップ

最低でも次を安全な場所へ保存してください。

- `yaju-upload.jks`
- keystore password
- key alias（既定: `yaju-upload`）
- key password

GitHubだけを唯一の保管先にしないでください。

### 3. GitHub Actions Secretsへ登録

GitHub Repository → Settings → Secrets and variables → Actions に次を登録します。

```text
YAJU_UPLOAD_KEYSTORE_B64
YAJU_STORE_PASSWORD
YAJU_KEY_ALIAS
YAJU_KEY_PASSWORD
```

`YAJU_UPLOAD_KEYSTORE_B64` には `android/signing/yaju-upload.jks.b64` の中身をそのまま登録します。

### 4. 署名済みAABを生成

GitHub Actionsから:

```text
Android Store Test AAB
→ Run workflow
```

Workflow内で次を自動確認します。

1. Secretsが揃っているか
2. keystore復元
3. upload certificateのSHA-1 / SHA-256表示
4. release AAB生成
5. `jarsigner -verify` による署名検証
6. AABをArtifactとして保存
7. runner上のkeystore削除

### 5. Play Console側

新規アプリではGoogle生成の **app signing key** を使う構成を推奨します。

重要なのは、upload keyとapp signing keyは別物という点です。

```text
upload key
  開発者が保持
  ↓ AABアップロード時の本人確認

app signing key
  Google Playが保持
  ↓ 実際にユーザーへ配るAPKの署名
```

### 6. Google Play GamesのSHA-1

Play Games Servicesの本番認証では、ローカルのupload keyだけではなく、
**Play Consoleに表示される app signing key のSHA-1** をGoogle Cloud / PGS側へ登録します。

Play Console:

```text
Google Playによる保護
→ Playアプリ署名
→ アプリ署名鍵の証明書
```

ローカルやGitHub Actionsから直接インストールしたAPKでもPGS認証させる場合は、
そのビルドを署名した証明書のSHA-1も別途登録します。
