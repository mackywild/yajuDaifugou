package com.example.daifugo.response;

import java.util.List;

/**
 * ゲーム全体の状態をクライアントへ返すレスポンスDTO。
 */
public class GameStateResponse {

    /** 部屋ID */
    private final String roomId;

    /** プレイヤー情報一覧 */
    private final List<PlayerResponse> players;

    /** 現在の手番プレイヤーID */
    private final String currentPlayerId;

    /** 場札情報 */
    private final FieldResponse field;

    /** 革命中か */
    private final boolean revolution;

    /** ゲーム開始済みか */
    private final boolean started;

    /** ゲーム終了済みか */
    private final boolean finished;

    /**
     * ゲーム状態レスポンスを生成する。
     *
     * @param roomId 部屋ID
     * @param players プレイヤー情報一覧
     * @param currentPlayerId 現在の手番プレイヤーID
     * @param field 場札情報
     * @param revolution 革命中の場合true
     * @param started ゲーム開始済みの場合true
     * @param finished ゲーム終了済みの場合true
     */
    public GameStateResponse(
            String roomId,
            List<PlayerResponse> players,
            String currentPlayerId,
            FieldResponse field,
            boolean revolution,
            boolean started,
            boolean finished) {

        this.roomId = roomId;
        this.players = players;
        this.currentPlayerId = currentPlayerId;
        this.field = field;
        this.revolution = revolution;
        this.started = started;
        this.finished = finished;
    }

    public String getRoomId() {
        return roomId;
    }

    public List<PlayerResponse> getPlayers() {
        return players;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public FieldResponse getField() {
        return field;
    }

    public boolean isRevolution() {
        return revolution;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }
}