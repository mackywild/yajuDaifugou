package com.example.daifugo.game.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ゲームの状態を持つ
 */
public class GameState {
    private final List<Player> players;

    private int currentPlayerIndex;
    private Integer lastPlayedPlayerIndex;

    private CardCombination fieldCombination;
    private boolean revolution;
    private GamePhase phase;
    private Mark lockedMark;

    /** 7渡し処理待ち。nullの場合は通常進行中。 */
    private PendingSevenTransfer pendingSevenTransfer;

    /** 全端末で共有するゲームイベント履歴。 */
    private final List<GameEvent> events = new ArrayList<>();

    /** 次に採番するイベントID。 */
    private long nextEventId = 1L;

    public GameState(List<Player> players) {
        Objects.requireNonNull(players, "players must not be null");

        if (players.size() < 2) {
            throw new IllegalArgumentException(
                "プレイヤーは2人以上必要です"
            );
        }

        if (players.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "players must not contain null"
            );
        }

        long distinctIdCount = players.stream()
            .map(Player::getId)
            .distinct()
            .count();

        if (distinctIdCount != players.size()) {
            throw new IllegalArgumentException(
                "プレイヤーIDが重複しています"
            );
        }

        this.players = new ArrayList<>(players);
        this.currentPlayerIndex = 0;
        this.phase = GamePhase.WAITING;
    }

    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Integer getLastPlayedPlayerIndex() {
        return lastPlayedPlayerIndex;
    }

    public CardCombination getFieldCombination() {
        return fieldCombination;
    }

    public boolean isRevolution() {
        return revolution;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void start() {
        if (phase != GamePhase.WAITING) {
            throw new IllegalStateException(
                "待機中のゲームのみ開始できます"
            );
        }

        phase = GamePhase.PLAYING;
    }

    public void updateField(
            CardCombination combination,
            int playedPlayerIndex
    ) {
        if (phase != GamePhase.PLAYING) {
            throw new IllegalStateException(
                "プレイ中ではありません"
            );
        }

        if (playedPlayerIndex < 0
                || playedPlayerIndex >= players.size()) {
            throw new IllegalArgumentException(
                "プレイヤー位置が不正です"
            );
        }

        this.fieldCombination = Objects.requireNonNull(
            combination,
            "combination must not be null"
        );

        this.lastPlayedPlayerIndex = playedPlayerIndex;
    }

    public void clearField() {
        this.fieldCombination = null;
        this.lastPlayedPlayerIndex = null;
        this.lockedMark = null;

        players.forEach(Player::clearPass);
    }

    public void toggleRevolution() {
        revolution = !revolution;
    }

    public int getFinishedPlayerCount() {
        return (int) players.stream()
            .filter(Player::hasFinished)
            .count();
    }

    public void finish() {
        phase = GamePhase.FINISHED;
    }

    /**
     * ゲームが終了済みか判定する。
     *
     * @return 終了済みの場合true
     */
    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }

    public void changeCurrentPlayer(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= players.size()) {
            throw new IllegalArgumentException(
                "プレイヤー位置が不正です"
            );
        }

        currentPlayerIndex = playerIndex;
    }

	public Mark getLockedMark() {
		return lockedMark;
	}

	public void setLockedMark(Mark lockedMark) {
		this.lockedMark = lockedMark;
	}
	
	public boolean isMarkLocked() {
		return lockedMark != null;
	}
	
	public void lockMark(Mark mark) {
		this.lockedMark = Objects.requireNonNull(
				mark,
				"マークが一致していません"
		);
	}
	
	public void clearMarkLock() {
		this.lockedMark = null;
	}

    /**
     * 7渡し処理が保留中か判定する。
     *
     * @return 7渡し待ちの場合true
     */
    public boolean hasPendingSevenTransfer() {
        return pendingSevenTransfer != null;
    }

    public PendingSevenTransfer getPendingSevenTransfer() {
        return pendingSevenTransfer;
    }

    /**
     * 7渡し処理を開始する。
     *
     * @param pendingSevenTransfer 7渡し保留状態
     */
    public void beginSevenTransfer(PendingSevenTransfer pendingSevenTransfer) {
        if (this.pendingSevenTransfer != null) {
            throw new IllegalStateException("既に7渡し処理が保留されています");
        }
        this.pendingSevenTransfer = Objects.requireNonNull(
            pendingSevenTransfer,
            "pendingSevenTransfer must not be null"
        );
    }

    /** 7渡し処理を完了し、保留状態を解除する。 */
    public void completeSevenTransfer() {
        if (pendingSevenTransfer == null) {
            throw new IllegalStateException("7渡し処理は保留されていません");
        }
        pendingSevenTransfer = null;
    }

    /**
     * 全端末へ共有するイベントを発行する。
     *
     * @param type イベント種別
     * @param player 対象プレイヤー
     * @return 発行したイベント
     */
    public GameEvent emitEvent(GameEventType type, Player player) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(player, "player must not be null");

        GameEvent event = new GameEvent(
            nextEventId++,
            type,
            player.getId(),
            player.getName()
        );
        events.add(event);
        return event;
    }

    /**
     * ゲームイベント履歴を取得する。
     * クライアントはイベントIDで重複再生を防止する。
     *
     * @return 読み取り専用イベント一覧
     */
    public List<GameEvent> getEvents() {
        return List.copyOf(events);
    }

}
