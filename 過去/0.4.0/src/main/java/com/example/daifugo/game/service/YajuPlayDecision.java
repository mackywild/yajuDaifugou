package com.example.daifugo.game.service;

/**
 * 野獣ルールによるプレイ判定結果。
 *
 * @param startsEightStep 最後の2枚から8を出し、10待ちへ移行する場合true
 * @param successfulFinish 8→10の野獣上がり成功となる場合true
 * @param penaltyFinish 野獣上がり違反の反則上がりとなる場合true
 */
public record YajuPlayDecision(
        boolean startsEightStep,
        boolean successfulFinish,
        boolean penaltyFinish
) {
    public static YajuPlayDecision none() {
        return new YajuPlayDecision(false, false, false);
    }
}
