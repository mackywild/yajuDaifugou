package com.example.daifugo.game.rule;
import java.util.Objects;

import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.Mark;

public class PlayValidator {

    public boolean canPlay(
            CardCombination selected,
            CardCombination field,
            boolean revolution,
            Mark lockedMark
            
    ) {
        Objects.requireNonNull(
            selected,
            "selected must not be null"
        );

        if (!selected.isValid()) {
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

        if (selected.getCardCount()
                != field.getCardCount()) {
            return false;
        }

        if (field.isSingleJoker()) {
            return selected.isSingleSpadeThree();
        }

        if (selected.isSingleJoker()) {
            return field.isSingle();
        }
        
        if (!matchesMarkLock(
        	         selected,
        	         lockedMark
        	 )) {
        	     return false;
        	 }

        int selectedStrength =
            selected.getBaseStrength();

        int fieldStrength =
            field.getBaseStrength();

        if (revolution) {
            return selectedStrength < fieldStrength;
        }

        return selectedStrength > fieldStrength;
    }
    
    private boolean matchesMarkLock(
    	         CardCombination selected,
    	         Mark lockedSuit
    	 ) {
    	     if (lockedSuit == null) {
    	         return true;
    	     }
    	
    	     if (selected.isSingleJoker()) {
    	         return true;
    	     }
    	
    	     return selected.getCards()
    	         .stream()
    	         .allMatch(card ->
    	             card.getSuit() == lockedSuit
    	         );
    	 }
}
