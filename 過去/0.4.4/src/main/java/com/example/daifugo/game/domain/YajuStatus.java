package com.example.daifugo.game.domain;

/**
 * 野獣ルールの進行状態。
 */
public enum YajuStatus {
    /** 野獣ルール未適用。 */
    NONE,

    /** 8と10を保持し、野獣上がりを狙う状態。 */
    ACTIVE,

    /** 最終手札が8×1 + 10×Nの状態から8を単体で出し、残り10待ちになった状態。 */
    EIGHT_PLAYED,

    /** 8→10の順で野獣上がりに成功した状態。 */
    COMPLETED,

    /** 野獣上がり条件を破って反則上がりした状態。 */
    PENALTY
}
