package com.example.daifugo.game.cpu;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

/** CPU難易度と戦略実装の対応を確認する。 */
class CpuStrategyFactoryTest {

    private final CpuStrategyFactory factory = new CpuStrategyFactory();

    @Test
    void easyCreatesEasyStrategy() {
        assertInstanceOf(EasyCpuStrategy.class, factory.create(CpuDifficulty.EASY));
    }

    @Test
    void normalCreatesNormalStrategy() {
        assertInstanceOf(NormalCpuStrategy.class, factory.create(CpuDifficulty.NORMAL));
    }

    @Test
    void hardCreatesHardStrategy() {
        assertInstanceOf(HardCpuStrategy.class, factory.create(CpuDifficulty.HARD));
    }

    @Test
    void ngodCreatesLearnedStrategy() {
        assertInstanceOf(NgodCpuStrategy.class, factory.create(CpuDifficulty.N_GOD));
    }
}
