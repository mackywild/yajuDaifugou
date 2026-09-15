package com.example.daifugo.game.cpu;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** N-GOD学習済みモデルの最小回帰テスト。 */
class NgodNeuralModelTest {

    @Test
    void predictionIsProbability() {
        double value = NgodNeuralModel.predict(new double[CpuFeatureExtractor.FEATURE_COUNT]);
        assertTrue(value >= 0.0 && value <= 1.0);
    }

    @Test
    void wrongFeatureCountIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NgodNeuralModel.predict(new double[CpuFeatureExtractor.FEATURE_COUNT - 1])
        );
    }
}
