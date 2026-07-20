package com.example.daifugo.game.rule;

public class RuleResult {
    private boolean fieldShouldClear;
    private boolean revolutionOccurred;

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
}
