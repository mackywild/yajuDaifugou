package com.example.daifugo.game.domain;

import java.util.Objects;

/**
 * 7渡しの保留状態。
 *
 * カードを出した直後にターンを進めず、指定枚数のカードを
 * 隣の未上がりプレイヤーへ渡し終えるまでゲーム進行を保留する。
 *
 * @param sourcePlayerId 渡す側のプレイヤーID
 * @param targetPlayerId 受け取る側のプレイヤーID
 * @param cardCount 渡す必要があるカード枚数
 * @param clearFieldAfterTransfer 7渡し完了後に場流しする場合true
 */
public record PendingSevenTransfer(
        String sourcePlayerId,
        String targetPlayerId,
        int cardCount,
        boolean clearFieldAfterTransfer
) {
    public PendingSevenTransfer {
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId must not be null");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId must not be null");
        if (cardCount <= 0) {
            throw new IllegalArgumentException("7渡し枚数は1枚以上で指定してください");
        }
    }
}
