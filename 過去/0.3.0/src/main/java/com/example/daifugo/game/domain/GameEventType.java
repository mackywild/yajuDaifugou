package com.example.daifugo.game.domain;

/**
 * 全プレイヤーへ共有するゲームイベント種別。
 * Android側ではこの種別を音声キューへ変換する。
 */
public enum GameEventType {
    /** 新たに野獣ルール対象者が発生した。 */
    YAJU_AVAILABLE,

    /** 野獣上がりに成功した。 */
    YAJU_SUCCESS
}
