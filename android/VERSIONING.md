# Android バージョン管理

野獣大富豪のAndroid版では、ユーザー向けバージョンとGoogle Play用の内部番号を分離します。

- `versionName`: リリースの意味を表す。Semantic Versioningベースで管理する。
  - 例: `0.6.0`, `0.6.1`, `0.7.0`
- `versionCode`: Google Play用の内部通し番号。必ず前回より大きくする。
  - GitHub ActionsのStore AABでは `1000 + github.run_number` を使用して自動採番する。

## 現在

- versionName: `0.6.0`
- ローカル既定versionCode: `10`
- Store AAB: CIがversionCodeを自動採番

## 運用

- バグ修正のみ: patchを上げる（例 `0.6.0 -> 0.6.1`）
- 後方互換の機能追加: minorを上げる（例 `0.6.x -> 0.7.0`）
- Play Consoleへの再アップロード都合だけではversionNameを変更しない
- `test01`, `test02` のようなアップロード回数をversionNameには使わない
