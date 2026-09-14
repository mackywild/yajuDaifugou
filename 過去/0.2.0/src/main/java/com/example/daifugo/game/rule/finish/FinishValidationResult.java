package com.example.daifugo.game.rule.finish;

public class FinishValidationResult {
    private final boolean allowed;
    private final String message;

    private FinishValidationResult(
            boolean allowed,
            String message
    ) {
        this.allowed = allowed;
        this.message = message;
    }

    public static FinishValidationResult allowed() {
        return new FinishValidationResult(
            true,
            null
        );
    }

    public static FinishValidationResult forbidden(
            String message
    ) {
        return new FinishValidationResult(
            false,
            message
        );
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMessage() {
        return message;
    }
}
