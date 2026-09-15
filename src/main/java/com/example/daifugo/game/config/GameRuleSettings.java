package com.example.daifugo.game.config;

/**
 * 対戦ごとの特殊ルール設定。
 *
 * CPU戦ではメニューから変更でき、マルチプレイはstandard()を使用する。
 */
public record GameRuleSettings(
        int jokerCount,
        boolean revolution,
        boolean eightCut,
        boolean markLock,
        boolean sevenTransfer,
        boolean yajuRule,
        boolean jackBack,
        boolean forbiddenFinish
) {
    public GameRuleSettings {
        if (jokerCount < 0 || jokerCount > 2) {
            throw new IllegalArgumentException("ジョーカー枚数は0～2枚で指定してください");
        }
        if (yajuRule && !eightCut) {
            throw new IllegalArgumentException("野獣ルールを有効にする場合は8切りも有効にしてください");
        }
    }

    /** 現行マルチプレイと同じ標準ルール。 */
    public static GameRuleSettings standard() {
        return new GameRuleSettings(
                1,
                true,
                true,
                true,
                true,
                true,
                true,
                true
        );
    }
}
