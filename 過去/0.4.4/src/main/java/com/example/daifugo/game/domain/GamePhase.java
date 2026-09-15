package com.example.daifugo.game.domain;

/**
 * ゲームの進行状態。
 */
public enum GamePhase {
    WAITING,
    PLAYING,
    FINISHED;

    /**
     * 現在ゲーム進行中か判定する。
     *
     * @return PLAYINGの場合true
     */
    public boolean isPlaying() {
        return this == PLAYING;
    }
}
