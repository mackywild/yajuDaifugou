# Android versioning

Androidの製品バージョンとGoogle Play用ビルド番号を分離して管理します。

## versionName

ユーザー向けの製品バージョンです。正本は `android/gradle.properties` の
`YAJU_VERSION_NAME` です。

現在:

```
YAJU_VERSION_NAME=0.5.0
```

Play ConsoleへAABを再投入しただけでは変更しません。
`test01` / `test02` のようなアップロード回数もversionNameには付けません。

- バグ修正版を製品として出す: patchを上げる（0.5.0 -> 0.5.1）
- 後方互換の機能追加を製品として出す: minorを上げる（0.5.x -> 0.6.0）

## versionCode

Google PlayがAABを識別する内部ビルド番号です。一度Playで使用した値は再利用しません。

Store AABのGitHub Actionsでは次の式で自動採番します。

```
100000 + GITHUB_RUN_NUMBER * 10 + GITHUB_RUN_ATTEMPT
```

これにより通常実行だけでなくActionsの再実行でも別versionCodeになります。

例:

```
versionName 0.5.0 / versionCode 100041
versionName 0.5.0 / versionCode 100051
versionName 0.5.0 / versionCode 100052  # run 5 の再実行
```

製品バージョンは同じ0.5.0のまま、Play用ビルド番号だけが増えます。
