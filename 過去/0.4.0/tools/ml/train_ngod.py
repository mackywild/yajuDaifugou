from __future__ import annotations
import csv, math, random, sys
from pathlib import Path
import numpy as np
import torch
from torch import nn
from torch.utils.data import DataLoader, TensorDataset

ROOT = Path(__file__).resolve().parents[2]
CSV_PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'tools/ml/ngod_selfplay.csv'
OUT_JAVA = ROOT / 'src/main/java/com/example/daifugo/game/cpu/NgodNeuralModel.java'
REPORT = ROOT / 'tools/ml/ngod_training_report.txt'
SEED = 8101919
random.seed(SEED); np.random.seed(SEED); torch.manual_seed(SEED)

arr = np.loadtxt(CSV_PATH, delimiter=',', skiprows=1, dtype=np.float32)
y = arr[:, :1]
X = arr[:, 1:]
assert X.shape[1] == 38, X.shape
idx = np.arange(len(X)); np.random.shuffle(idx)
cut = int(len(idx)*0.9)
tr, va = idx[:cut], idx[cut:]
Xtr, ytr = torch.from_numpy(X[tr]), torch.from_numpy(y[tr])
Xva, yva = torch.from_numpy(X[va]), torch.from_numpy(y[va])

class Model(nn.Module):
    def __init__(self):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(38, 32), nn.ReLU(),
            nn.Linear(32, 16), nn.ReLU(),
            nn.Linear(16, 1), nn.Sigmoid(),
        )
    def forward(self, x): return self.net(x)

model = Model()
opt = torch.optim.AdamW(model.parameters(), lr=2e-3, weight_decay=1e-4)
lossfn = nn.MSELoss()
loader = DataLoader(TensorDataset(Xtr,ytr), batch_size=2048, shuffle=True)
lines=[]
for epoch in range(1, 13):
    model.train(); total=0; n=0
    for xb,yb in loader:
        opt.zero_grad(); pred=model(xb); loss=lossfn(pred,yb); loss.backward(); opt.step()
        total += loss.item()*len(xb); n += len(xb)
    model.eval()
    with torch.no_grad():
        vp=model(Xva); vl=lossfn(vp,yva).item()
        mae=(vp-yva).abs().mean().item()
        corr=np.corrcoef(vp.squeeze().numpy(), yva.squeeze().numpy())[0,1]
    line=f'epoch={epoch:02d} train_mse={total/n:.6f} val_mse={vl:.6f} val_mae={mae:.6f} corr={corr:.4f}'
    print(line); lines.append(line)

# Export weights.
mods=[m for m in model.net if isinstance(m, nn.Linear)]
weights=[m.weight.detach().cpu().numpy().astype(np.float64) for m in mods]
biases=[m.bias.detach().cpu().numpy().astype(np.float64) for m in mods]

def arr1(name,a):
    vals=', '.join(f'{float(v):.10g}' for v in a.ravel())
    return f'    private static final double[] {name} = new double[]{{{vals}}};\n'

def arr2(name,a):
    flat=', '.join(f'{float(v):.10g}' for v in a.ravel())
    return f'    private static final double[] {name} = new double[]{{{flat}}};\n'

java='''package com.example.daifugo.game.cpu;\n\n/**\n * N-GODの自己対戦学習済みニューラル評価モデル。\n *\n * 5,000ゲーム・約27万state-actionサンプルをMonte Carlo最終順位報酬で学習。\n * tools/ml/train_ngod.py から再生成できる。\n */\npublic final class NgodNeuralModel {\n    private NgodNeuralModel() {}\n\n'''
java += arr2('W1', weights[0]) + arr1('B1', biases[0])
java += arr2('W2', weights[1]) + arr1('B2', biases[1])
java += arr2('W3', weights[2]) + arr1('B3', biases[2])
java += '''\n    public static double predict(double[] x) {\n        if (x.length != CpuFeatureExtractor.FEATURE_COUNT) {\n            throw new IllegalArgumentException("N-GOD特徴量数が一致しません");\n        }\n        double[] h1 = denseRelu(x, W1, B1, 32);\n        double[] h2 = denseRelu(h1, W2, B2, 16);\n        double z = B3[0];\n        for (int i = 0; i < 16; i++) z += W3[i] * h2[i];\n        return 1.0 / (1.0 + Math.exp(-z));\n    }\n\n    private static double[] denseRelu(double[] input, double[] weights, double[] bias, int outputSize) {\n        int inputSize = input.length;\n        double[] out = new double[outputSize];\n        for (int o = 0; o < outputSize; o++) {\n            double v = bias[o];\n            int base = o * inputSize;\n            for (int i = 0; i < inputSize; i++) v += weights[base + i] * input[i];\n            out[o] = Math.max(0.0, v);\n        }\n        return out;\n    }\n}\n'''
OUT_JAVA.write_text(java, encoding='utf-8')
REPORT.write_text('\n'.join(lines)+f'\nrows={len(X)} train={len(tr)} val={len(va)} seed={SEED}\n', encoding='utf-8')
print('wrote', OUT_JAVA)
