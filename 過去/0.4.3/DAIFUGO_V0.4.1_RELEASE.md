# Daifugo v0.4.1 — CPU MOTION UPDATE

## 概要

v0.4.0のCPU戦で、CPUが高速に連続行動して何を出したか確認できない問題を改善するUIアップデート。

## 変更点

- CPUのカード提出を約3秒のモーションとして表示
- CPU PASS表示
- CPU 7渡し表示
- 直近4手のCPU行動ログ
- CPU演出中の人間操作ロック
- CPU初手の場合もゲーム開始直後から演出

## ゲームロジック

GameEngine / RuleEngine / CPU戦略そのものの判定仕様は変更しない。
LocalCpuGameManagerを1手単位で実行可能にし、ViewModelが演出時間を管理する。
