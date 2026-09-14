# N-GOD Machine Learning

Daifugo v0.4.0の最上位CPU「N-GOD」の自己対戦学習ツールです。

## 仕組み

1. `SelfPlayDataGenerator.java` が2〜4人の自己対戦を実行
2. 各選択手について38次元のstate-action特徴量を保存
3. 最終順位を0〜1の報酬へ変換
4. `train_ngod.py` がMLPを学習
5. 学習済み重みを `NgodNeuralModel.java` として直接export

ゲーム実行時にはPython / PyTorchは不要です。

## データ生成

ゲームコアをコンパイルしたクラスパスで:

```bash
java SelfPlayDataGenerator 5000 tools/ml/ngod_selfplay.csv
```

`ngod_selfplay.csv` は約69MBになるためGit管理対象外です。`ngod_selfplay_sample.csv` は形式確認用サンプルとして同梱しています。

## 学習

```bash
python -m pip install -r tools/ml/requirements.txt
python tools/ml/train_ngod.py tools/ml/ngod_selfplay.csv
```

学習後:

- `src/main/java/com/example/daifugo/game/cpu/NgodNeuralModel.java` 更新
- `tools/ml/ngod_training_report.txt` 更新

## Benchmark

```bash
java CpuBenchmark 4000
```

基準は各難易度CPU1体 vs Hard CPU3体です。
