package com.example.daifugo.response;

import com.example.daifugo.game.domain.GameEvent;

/**
 * 全端末へ共有するゲームイベントレスポンス。
 */
public class GameEventResponse {
    private final long id;
    private final String type;
    private final String playerId;
    private final String playerName;

    public GameEventResponse(
            long id,
            String type,
            String playerId,
            String playerName
    ) {
        this.id = id;
        this.type = type;
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public static GameEventResponse from(GameEvent event) {
        return new GameEventResponse(
            event.id(),
            event.type().name(),
            event.playerId(),
            event.playerName()
        );
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
}
