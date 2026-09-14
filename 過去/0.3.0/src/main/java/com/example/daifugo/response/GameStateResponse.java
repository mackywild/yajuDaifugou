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

    /** 場で固定されているマーク。縛りがない場合はnull */
    private final String lockedMark;

    /** リクエストしたプレイヤーがホストか */
    private final boolean host;

    /** ゲーム開始済みか */
    private final boolean started;

    /** ゲーム終了済みか */
    private final boolean finished;

    /** 7渡し保留状態 */
    private final SevenTransferResponse sevenTransfer;

    /** 全端末共有イベント履歴 */
    private final List<GameEventResponse> events;

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
            String lockedMark,
            boolean host,
            boolean started,
            boolean finished,
            SevenTransferResponse sevenTransfer,
            List<GameEventResponse> events) {

        this.roomId = roomId;
        this.players = players;
        this.currentPlayerId = currentPlayerId;
        this.field = field;
        this.revolution = revolution;
        this.lockedMark = lockedMark;
        this.host = host;
        this.started = started;
        this.finished = finished;
        this.sevenTransfer = sevenTransfer;
        this.events = List.copyOf(events);
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

    public String getLockedMark() {
        return lockedMark;
    }

    public boolean isHost() {
        return host;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return finished;
    }

    public SevenTransferResponse getSevenTransfer() {
        return sevenTransfer;
    }

    public List<GameEventResponse> getEvents() {
        return events;
    }
}