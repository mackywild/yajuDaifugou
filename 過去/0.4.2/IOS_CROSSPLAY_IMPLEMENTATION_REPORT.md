# Daifugo v0.3.0 iOS Crossplay 実装レポート

## 追加

- iPhone向けSwiftUIクライアント
- REST Cookieセッション / CSRF対応
- URLSessionWebSocketTaskによる状態変更通知
- 5秒フォールバック同期
- Login / Lobby / Room / Game / Result
- 手札複数選択、カード提出、パス
- 7渡し選択UI
- 革命・縛り・野獣状態表示
- 野獣共有イベント音声
- LANローカルネットワーク権限 / ATS設定
- GitHub Actions iOS Simulatorビルド

## クロスプレイ

PCブラウザ、Android、iPhoneはいずれも同じSpring Bootサーバーへ接続し、同一GameRoomとGameEngineを使用する。
クライアントにルール判定を持たせないため、プラットフォーム差による判定差は発生しない。

## 実機配布について

iPhoneへの実機インストールにはAppleのコード署名が必要。XcodeでApple ID/Teamを設定すれば自身の端末へRunできる。
App Store/TestFlight配布を行う場合はApple Developer Program側の証明書・Provisioning設定を追加する。
