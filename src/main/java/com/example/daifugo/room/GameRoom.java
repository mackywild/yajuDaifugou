package com.example.daifugo.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngine;

/**
 * 大富豪の対戦部屋を表すクラス。
 *
 * 部屋に参加しているプレイヤー、
 * ゲーム状態およびゲーム進行処理を保持する。
 */
public class GameRoom {

    /** 1部屋に参加できる最大人数 */
    private static final int MAX_PLAYER_COUNT = 4;

    /** 部屋を一意に識別するID */
    private final String roomId;

    /** 部屋に参加しているプレイヤー一覧 */
    private final List<Player> players;

    /** ゲームの現在状態 */
    private GameState gameState;

    /** ゲーム進行処理 */
    private GameEngine gameEngine;

    /** ゲーム開始済みか */
    private boolean started;

    /** 部屋を作成したホストのプレイヤーID */
    private String hostPlayerId;

    /** 最終アクセス日時（epoch milli） */
    private volatile long lastAccessedAt;

    /**
     * 対戦部屋を生成する。
     *
     * @param roomId 部屋ID
     */
    public GameRoom(String roomId) {
        this.roomId = Objects.requireNonNull(
                roomId,
                "roomId must not be null"
        );
        this.players = new ArrayList<>();
        this.started = false;
        this.lastAccessedAt = System.currentTimeMillis();
    }

    /**
     * 部屋IDを取得する。
     *
     * @return 部屋ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * 参加プレイヤー一覧を取得する。
     *
     * 外部から直接変更されないよう、
     * 読み取り専用のリストを返す。
     *
     * @return 参加プレイヤー一覧
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * ゲーム状態を取得する。
     *
     * @return ゲーム状態
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * ゲームエンジンを取得する。
     *
     * @return ゲームエンジン
     */
    public GameEngine getGameEngine() {
        return gameEngine;
    }

    /**
     * プレイヤーを部屋へ追加する。
     *
     * @param player 追加するプレイヤー
     */
    public void addPlayer(Player player) {

        Objects.requireNonNull(
                player,
                "player must not be null"
        );

        if (started) {
            throw new IllegalStateException(
                    "ゲーム開始後は参加できません"
            );
        }

        if (isFull()) {
            throw new IllegalStateException(
                    "部屋は満員です"
            );
        }

        if (containsPlayer(player.getId())) {
            throw new IllegalArgumentException(
                    "同じプレイヤーが既に参加しています"
            );
        }

        players.add(player);

        if (hostPlayerId == null) {
            hostPlayerId = player.getId();
        }

        touch();
    }

    /**
     * 指定されたプレイヤーを退出させる。
     *
     * @param playerId プレイヤーID
     * @return 削除できた場合true
     */
    public boolean removePlayer(String playerId) {

        if (started && !isFinished()) {
            throw new IllegalStateException(
                    "対戦中は退出できません。ゲーム終了後に退出してください"
            );
        }

        boolean removed = players.removeIf(
                player -> player.getId().equals(playerId)
        );

        if (removed && Objects.equals(hostPlayerId, playerId)) {
            hostPlayerId = players.isEmpty()
                    ? null
                    : players.get(0).getId();
        }

        if (removed) {
            touch();
        }

        return removed;
    }

    /**
     * 指定されたプレイヤーが参加済みか判定する。
     *
     * @param playerId プレイヤーID
     * @return 参加済みの場合true
     */
    public boolean containsPlayer(String playerId) {
        return players.stream()
                .anyMatch(player ->
                        player.getId().equals(playerId)
                );
    }

    /**
     * 部屋が満員か判定する。
     *
     * @return 最大人数に達している場合true
     */
    public boolean isFull() {
        return players.size() >= MAX_PLAYER_COUNT;
    }

    /**
     * 現在の参加人数を取得する。
     *
     * @return 参加人数
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * ゲームが開始済みか判定する。
     *
     * @return 開始済みの場合true
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * ゲームを開始する。
     *
     * ゲーム状態とゲームエンジンを設定し、
     * 部屋を開始済み状態へ変更する。
     *
     * @param gameState 初期化済みゲーム状態
     * @param gameEngine ゲームエンジン
     */
    public void start(
            GameState gameState,
            GameEngine gameEngine
    ) {
        if (started) {
            throw new IllegalStateException(
                    "ゲームは既に開始されています"
            );
        }

        this.gameState = Objects.requireNonNull(
                gameState,
                "gameState must not be null"
        );

        this.gameEngine = Objects.requireNonNull(
                gameEngine,
                "gameEngine must not be null"
        );

        this.started = true;
        touch();
    }


    /**
     * ゲームが終了済みか判定する。
     *
     * @return 終了済みの場合true
     */
    public boolean isFinished() {
        return started
                && gameState != null
                && gameState.isFinished();
    }

    /**
     * ホストのプレイヤーIDを取得する。
     *
     * @return ホストのプレイヤーID
     */
    public String getHostPlayerId() {
        return hostPlayerId;
    }

    /**
     * 指定プレイヤーがホストか判定する。
     *
     * @param playerId プレイヤーID
     * @return ホストの場合true
     */
    public boolean isHost(String playerId) {
        return Objects.equals(hostPlayerId, playerId);
    }

    /**
     * 部屋の最終アクセス日時を更新する。
     */
    public void touch() {
        lastAccessedAt = System.currentTimeMillis();
    }

    /**
     * 最終アクセス日時を取得する。
     *
     * @return epoch milli
     */
    public long getLastAccessedAt() {
        return lastAccessedAt;
    }
}
