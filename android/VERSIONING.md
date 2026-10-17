# Android バージョン管理

Android版では、製品バージョンとGoogle Play用ビルド番号を分離して管理します。

## versionName

ユーザーが見る製品バージョンです。現在は `0.5.0`。

`android/gradle.properties` の `YAJU_VERSION_NAME` を正本とします。

- 内部テストAABを作り直しただけでは変更しない
- バグ修正版を製品として出すときは patch を上げる
- 後方互換の機能追加を製品として出すときは minor を上げる
- `test01` / `test02` のようなアップロード回数は付けない

## versionCode

Google Playが識別する内部ビルド番号です。過去に使用した値は再利用しません。

- ローカル既定値は `android/gradle.properties` の `YAJU_VERSION_CODE`
- Store AABのGitHub Actionsでは `1000 + GITHUB_RUN_NUMBER` を自動採番
- Play Consoleへ再投入しても `versionName` はそのまま、`versionCode` だけ新しくなる

例:

```
versionName 0.5.0 / versionCode 1007
versionName 0.5.0 / versionCode 1008
versionName 0.5.0 / versionCode 1009
```

これは同じ0.5.0の内部テストビルド違いです。製品バージョンを増やしたことにはなりません。
