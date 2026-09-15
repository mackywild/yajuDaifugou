package com.example.daifugo.game.cpu;

/** CPU戦の難易度。 */
public enum CpuDifficulty {
    EASY("簡単"),
    NORMAL("普通"),
    HARD("難しい"),
    N_GOD("N-GOD");

    private final String displayName;

    CpuDifficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
