package com.example.daifugo.game.cpu;

/** 難易度からCPU思考ロジックを生成する。 */
public class CpuStrategyFactory {
    public CpuStrategy create(CpuDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new EasyCpuStrategy();
            case NORMAL -> new NormalCpuStrategy();
            case HARD -> new HardCpuStrategy();
            case N_GOD -> new NgodCpuStrategy();
        };
    }
}
