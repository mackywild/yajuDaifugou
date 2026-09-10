package com.example.daifugo.game.domain;

import java.util.Objects;

/**
 * ゲーム中に発生した全体共有イベント。
 *
 * @param id ゲーム内で単調増加するイベントID
 * @param type イベント種別
 * @param playerId 対象プレイヤーID
 * @param playerName 対象プレイヤー名
 */
public record GameEvent(
        long id,
        GameEventType type,
        String playerId,
        String playerName
) {
    public GameEvent {
        if (id <= 0) {
            throw new IllegalArgumentException("イベントIDは1以上で指定してください");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(playerName, "playerName must not be null");
    }
}
