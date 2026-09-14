package com.example.daifugo.response;

import java.util.Collections;
import java.util.List;

import com.example.daifugo.game.domain.CardCombination;

/**
 * 場に出ているカード情報を返すレスポンスDTO。
 */
public class FieldResponse {

    /** 場のカード */
    private final List<CardResponse> cards;

    /** 場の役 */
    private final String combinationType;

    /**
     * 場札レスポンスを生成する。
     *
     * @param cards 場のカード
     * @param combinationType 場の役
     */
    public FieldResponse(
            List<CardResponse> cards,
            String combinationType) {

        this.cards = cards;
        this.combinationType = combinationType;
    }

    /**
     * CardCombinationからレスポンスDTOを生成する。
     *
     * @param combination 場のカード組み合わせ
     * @return 場札レスポンス
     */
    public static FieldResponse from(
            CardCombination combination) {

        if (combination == null) {
            return new FieldResponse(
                    Collections.emptyList(),
                    null);
        }

        List<CardResponse> cards =
                combination.getCards()
                        .stream()
                        .map(CardResponse::from)
                        .toList();

        return new FieldResponse(
                cards,
                combination.getType().name());
    }

    public List<CardResponse> getCards() {
        return cards;
    }

    public String getCombinationType() {
        return combinationType;
    }
}