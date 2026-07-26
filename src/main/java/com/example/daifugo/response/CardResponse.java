package com.example.daifugo.response;

import com.example.daifugo.game.domain.Card;

/**
 * カード情報をクライアントへ返すレスポンスDTO。
 */
public class CardResponse {

    /** カードのスート */
    private final String suit;

    /** カードのランク */
    private final String rank;

    /** ジョーカーかどうか */
    private final boolean joker;

    /**
     * カードレスポンスを生成する。
     *
     * @param suit カードのスート
     * @param rank カードのランク
     * @param joker ジョーカーの場合true
     */
    public CardResponse(
            String suit,
            String rank,
            boolean joker) {

        this.suit = suit;
        this.rank = rank;
        this.joker = joker;
    }

    /**
     * CardからレスポンスDTOを生成する。
     *
     * @param card 変換対象のカード
     * @return カードレスポンス
     */
    public static CardResponse from(Card card) {

        if (card == null) {
            throw new IllegalArgumentException(
                    "カードはnullにできません。");
        }

        return new CardResponse(
                card.getSuit().name(),
                card.getRank().name(),
                card.isJoker());
    }

    /**
     * スートを取得する。
     *
     * @return スート
     */
    public String getSuit() {
        return suit;
    }

    /**
     * ランクを取得する。
     *
     * @return ランク
     */
    public String getRank() {
        return rank;
    }

    /**
     * ジョーカーか判定する。
     *
     * @return ジョーカーの場合true
     */
    public boolean isJoker() {
        return joker;
    }
}