package com.example.daifugo.game.rule;

import java.util.Objects;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.Mark;

/**
 * 選択したカードを現在の場へ提出できるか判定する。
 */
public class PlayValidator {

    /**
     * 選択した組み合わせが現在の場へ提出可能か判定する。
     *
     * @param selected 選択したカード組み合わせ
     * @param field 現在の場札。場が空の場合はnull
     * @param strengthReversed 革命/Jバックを合成した実効的な強弱反転状態
     * @param lockedMark 縛り中のマーク。縛りがない場合はnull
     * @return 提出可能な場合true
     */
    public boolean canPlay(
            CardCombination selected,
            CardCombination field,
            boolean strengthReversed,
            Mark lockedMark
    ) {
        return canPlay(
            selected,
            field,
            strengthReversed,
            lockedMark,
            false
        );
    }

    /**
     * 選択した組み合わせが現在の場へ提出可能か判定する。
     *
     * @param selected 選択したカード組み合わせ
     * @param field 現在の場札。場が空の場合はnull
     * @param strengthReversed 革命/Jバックを合成した実効的な強弱反転状態
     * @param lockedMark 縛り中のマーク。縛りがない場合はnull
     * @param spadeThreeJokerReturnActive JOKERをスペード3で返した直後か
     * @return 提出可能な場合true
     */
    public boolean canPlay(
            CardCombination selected,
            CardCombination field,
            boolean strengthReversed,
            Mark lockedMark,
            boolean spadeThreeJokerReturnActive
    ) {
        Objects.requireNonNull(
            selected,
            "selected must not be null"
        );

        if (!selected.isValid()) {
            return false;
        }

        if (spadeThreeJokerReturnActive) {
            if (field == null || !field.isSingleSpadeThree()) {
                throw new IllegalStateException(
                    "スペード3のJOKER返し状態と場札が一致していません"
                );
            }
            return false;
        }

        if (field == null) {
            return true;
        }

        if (!field.isValid()) {
            throw new IllegalArgumentException(
                "場の組み合わせが不正です"
            );
        }

        if (selected.getType() != field.getType()) {
            return false;
        }

        if (selected.getCardCount() != field.getCardCount()) {
            return false;
        }

        if (field.isSingleJoker()) {
            return selected.isSingleSpadeThree();
        }

        if (selected.isSingleJoker()) {
            return field.isSingle();
        }

        if (!matchesMarkLock(selected, lockedMark)) {
            return false;
        }

        int selectedStrength = selected.getBaseStrength();
        int fieldStrength = field.getBaseStrength();

        if (strengthReversed) {
            return selectedStrength < fieldStrength;
        }

        return selectedStrength > fieldStrength;
    }

    /** マーク縛りに適合するか判定する。 */
    private boolean matchesMarkLock(
            CardCombination selected,
            Mark lockedMark
    ) {
        if (lockedMark == null) {
            return true;
        }

        if (selected.isSingleJoker()) {
            return true;
        }

        return selected.getCards()
            .stream()
            .allMatch(card ->
                card.isJoker()
                    || card.getSuit() == lockedMark
            );
    }
}
