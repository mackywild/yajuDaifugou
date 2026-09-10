package com.example.daifugo.game.rule;

import java.util.Objects;

import com.example.daifugo.game.domain.Mark;

public class RuleResult {
    private boolean fieldShouldClear;
    private boolean revolutionOccurred;
    private Mark markToLock;

    public boolean shouldClearField() {
        return fieldShouldClear;
    }

    public boolean isRevolutionOccurred() {
        return revolutionOccurred;
    }

    public void requestFieldClear() {
        this.fieldShouldClear = true;
    }

    public void markRevolutionOccurred() {
        this.revolutionOccurred = true;
    }
    
    public void requestMarkLock(Mark mark) {
    	this.markToLock = Objects.requireNonNull(
    			mark,
    			"マークがありません"
    	);
    }
    
    public boolean shouldLockMark() {
         return markToLock != null;
     }
    
    public Mark getMarkToLock() {
         return markToLock;
     }
    
}
