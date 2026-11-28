# Macky UI v2.0.2 第2回レビュー対応

対象レビュー: 6097687488 / RV-06a, RV-07a
修正・検証対象HEAD: 135e828b9e54e6f0f28d1b7f660d40cb5b8b063e

## 修正

|ID|対応|
|---|---|
|RV-06a|入力・フォーム・Advanced Gridの操作可否を初期値と分離。disabled/readonly、個別field/columnのフラグをライブ反映。値、履歴、個別選択肢の元のdisabledを保持。フォームの通信完了後もロック保持。グリッドの変更系API、Undo/Redo/reset、CSV確認中の権限変更を制御。未確定のセル編集が権限を失った場合は取消通知。|
|RV-07a|セル選択と行ID選択を分離。遅延・通常再描画でrowKeyによるチェックとフォーカス復元。存在しないIDのみ除去。選択総数と表示中件数、全選択解除を追加。非表示行誤削除を避けるため、行削除は表示中のみ。|
|周辺修正|mountScreenがsubmitTaskを参照するが引数で受け取っていなかったため修正。生成HTML/JSの実行・部品ID付き保存関数接続に影響。|

## 検証結果

GitHubの13個の実JSモジュールを取得し、import/exportを変換したJS隔離環境で実行した。前回追加の8件の実テスト本体を再実行し8/8合格。追加6件は実関数にDOM/イベント/タイマー/Vue runtimeのスタブを渡して実行し6/6合格。合計14/14。

|追加観点|結果|
|---|---|
|入力のdisabled/readonly切替、値保持、選択肢固有disabled|PASS（DOMスタブ）|
|フォーム非同期保存中のロック、完了後の維持、個別disabled|PASS（DOMスタブ）|
|グリッド変更禁止、CSV確認競合、履歴保持、未確定入力取消|PASS（DOMスタブ）|
|行チェック選択・フォーカス、ページ移動、非表示行削除防止、Undo|PASS（DOM/イベント/タイマースタブ）|
|mountScreen保存関数への値・部品ID伝播|PASS（DOMスタブ）|
|Vue optionsのライブ無効化・値保持・resetKey|PASS（Vue runtime/DOMスタブ）|

追加した実ブラウザ回帰試験は demo/browser-review.js に6件（直接API 5件、Vue 1件）。従来8件を維持して合計14件、Vue未導入時は3件skip。構文は変換したAsyncFunctionによる評価で確認。実ブラウザ・実Vueの動作保証ではない。

## 未確認

実Node.js/npm、HTTP、DOM/CSS、実ポインター、実Vue、IME、Excel、生成HTML/SFCの実ビルド・配布は引き続き未実施。前回の環境制約（GitHub直接取得不可）により今回も隔離検証を実施。手元で npm install / npm run check / npm test / npm run build / npm pack / npm start を実行し、Test Studioの旧・v2・レビュー回帰試験を確認する。

## 仕様決定

選択状態を検索・ページ移動で保持するため、行削除は表示中の選択行だけに限定。表示外の選択行を削除する必要がある場合は別ページで明示操作。readonlyは入力部品・フォーム・Advanced Gridに定義。その他の部品はdisabledをinertで反映。クライアント側アクセスは認可の代替ではない。Vueの初期値とそれ以外の設定は引き続きresetKeyで反映。

コード修正済み、再レビュー待ち。実装者によるスレッド解決・マージは行わない。すべて tools/macky-ui 配下の修正であり野獣大富豪に導入しない。
