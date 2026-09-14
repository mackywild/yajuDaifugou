package com.example.daifugo.game.rule;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.CombinationType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;

public class MarkLockRule implements Rule {

    @Override
    public boolean matches(
            GameState state,
            Player player,
            CardCombination previousField,
            CardCombination combination
    ) {
        if (previousField == null) {
            return false;
        }

        if (state.isMarkLocked()) {
            return false;
        }

        if (previousField.getType()
                != CombinationType.SINGLE) {
            return false;
        }

        if (combination.getType()
                != CombinationType.SINGLE) {
            return false;
        }

        Card previousCard =
            previousField.getCards().get(0);

        Card selectedCard =
            combination.getCards().get(0);

        if (previousCard.isJoker()
                || selectedCard.isJoker()) {
            return false;
        }

        return previousCard.getSuit()
            == selectedCard.getSuit();
    }

    @Override
    public void apply(
            GameState state,
            Player player,
            CardCombination previousField,
            CardCombination combination,
            RuleResult result
    ) {
        Mark suit =
            combination.getCards()
                .get(0)
                .getSuit();

        result.requestMarkLock(suit);
    }
}
