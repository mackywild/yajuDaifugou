package com.example.daifugo.response;

import com.example.daifugo.game.domain.PendingSevenTransfer;

/**
 * 7渡し保留状態をクライアントへ返すDTO。
 */
public class SevenTransferResponse {
    private final boolean pending;
    private final String sourcePlayerId;
    private final String targetPlayerId;
    private final int cardCount;

    public SevenTransferResponse(
            boolean pending,
            String sourcePlayerId,
            String targetPlayerId,
            int cardCount
    ) {
        this.pending = pending;
        this.sourcePlayerId = sourcePlayerId;
        this.targetPlayerId = targetPlayerId;
        this.cardCount = cardCount;
    }

    public static SevenTransferResponse none() {
        return new SevenTransferResponse(false, null, null, 0);
    }

    public static SevenTransferResponse from(PendingSevenTransfer pending) {
        if (pending == null) {
            return none();
        }
        return new SevenTransferResponse(
            true,
            pending.sourcePlayerId(),
            pending.targetPlayerId(),
            pending.cardCount()
        );
    }

    public boolean isPending() { return pending; }
    public String getSourcePlayerId() { return sourcePlayerId; }
    public String getTargetPlayerId() { return targetPlayerId; }
    public int getCardCount() { return cardCount; }
}
