package com.example.daifugo.game.cpu;

import java.util.List;

import com.example.daifugo.game.domain.Card;

/** CPUが選択した1手。 */
public record CpuMove(boolean pass, List<Card> cards) {
    public CpuMove {
        cards = List.copyOf(cards);
        if (pass && !cards.isEmpty()) {
            throw new IllegalArgumentException("パスにカードは指定できません");
        }
    }

    public static CpuMove passMove() {
        return new CpuMove(true, List.of());
    }

    public static CpuMove playCards(List<Card> cards) {
        return new CpuMove(false, cards);
    }
}
