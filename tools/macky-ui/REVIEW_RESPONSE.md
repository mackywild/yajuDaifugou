# Macky UI v2.0.1 レビュー指摘対応報告

対象PR: #42 / レビュー対象HEAD 54aa13f2a36333081e85f4622850fc16ab9d907f
修正実装・回帰試験対象HEAD: d3cb038854e6a3a6fbca0e3c7d7e7e0617821b3f

|ID|対応|実行確認|
|---|---|---|
|RV-01|既定無効、全列・必須・型・権限、既存ID型維持、差分確認、競合拒否、原子適用|実モデル回帰合格、ブラウザ確認待ち|
|RV-02|Vue submitTask propとPromise待機、成功通知分離、生成JS/SFC接続|実renderアダプター＋スタブ合格、実Vue待ち|
|RV-03|日付・時刻の型別検証、境界、うるう年、深夜跨ぎ|実モデル回帰合格、ネイティブ入力待ち|
|RV-04|外部TSV安全化、内部MIME raw|実変換関数合格、Clipboard/Excel待ち|
|RV-05|compositionとisComposing/229でキー処理抑止|構文・静的確認、ブラウザ/実IME待ち|
|RV-06|状態保持、明示resetKey、busy時再生成拒否|実Vueアダプター＋スタブ合格、実Vue待ち|
|RV-07|ID/列キーによる復元、blur再描画を操作完了後に遅延|構文・静的確認、実ブラウザ待ち|

## 実行済み

GitHub連携で修正ソースを読み戻し、JavaScript隔離実行環境でimport/export宣言を変換して13モジュールを評価。追加Nodeテスト test/review.test.js の実際の8テスト本体を同環境のassert/test互換ハーネスで実行し8件合格。加えてrenderアダプター（フォームの生成のみスタブ）によるPromise待機・reject伝播、およびVueアダプター（Vue runtimeと部品生成のみスタブ）の状態保持・明示reset・busy拒否を各1件、合計10件合格。実Node test runner、実Vue/DOMによる実行ではない。修正JSと追加試験の構文も変換評価で確認済み。

## 未実施

コンテナからGitHubへの git ls-remote が Could not resolve host: github.com で失敗。リポジトリ取得ができないため、実際の npm run check/test/build/pack、HTTP配信、ブラウザ回帰試験、実Vue、実IME、Excel、他アプリ導入は未実施。マージは保留。

## 手元受入

    git fetch origin
    git switch feature/macky-ui-library
    git pull --ff-only
    cd tools/macky-ui
    npm install
    npm run check
    npm test
    npm run build
    npm pack
    npm start

Test Studioの旧・v2ブラウザ試験と「レビュー回帰試験」を実施（後者は8項目、Vueなしでは2項目skip）。Playground「csv-import.html」で取消・権限違反・差分承認を確認。実際の日本語IMEで変換確定Enter/Escape、ソート列編集後フォーカス、マウスによる別セル/ボタン押下、実Excelへの文字列貼付、生成Vue SFC/配布パッケージを確認する。

## 意図的な仕様変更

CSV既定無効・非同期化・追加/削除は明示許可。状態保持Vue部品はoptionsを初期値として扱い、resetKeyで再生成。フォームの保存処理はsubmitTaskへ移動（@actionは通知）。安全性のための変更で、旧API利用者は適用前に確認する。Macky UIのみの変更であり野獣大富豪への導入はなし。

実装者による対応報告。レビューの承認・スレッド解決・マージは実施しない。
