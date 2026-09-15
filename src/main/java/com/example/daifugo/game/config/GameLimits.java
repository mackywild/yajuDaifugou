package com.example.daifugo.game.config;

/**
 * 対戦人数に関する共通上限・下限。
 *
 * サーバー対戦とローカルCPU戦で同じ制約を利用し、
 * 端末ごとの人数上限の食い違いを防ぐ。
 */
public final class GameLimits {

    /** 1ゲームを開始できる最小人数。 */
    public static final int MIN_PLAYER_COUNT = 2;

    /** 1ゲームに参加できる最大人数。 */
    public static final int MAX_PLAYER_COUNT = 8;

    /** 人間1人のCPU戦で選択できる最大CPU人数。 */
    public static final int MAX_CPU_COUNT = MAX_PLAYER_COUNT - 1;

    private GameLimits() {
        // 定数クラスのためインスタンス化しない。
    }
}
