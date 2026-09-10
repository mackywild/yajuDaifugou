package com.example.daifugo.response;

import java.util.Collections;
import java.util.List;

import com.example.daifugo.game.domain.Player;

/**
 * プレイヤー情報をクライアントへ返すレスポンスDTO。
 *
 * 自分自身の場合のみ手札内容を保持し、
 * 他プレイヤーの場合は手札枚数のみ保持する。
 */
public class PlayerResponse {

    /** プレイヤーID */
    private final String playerId;

    /** プレイヤー名 */
    private final String playerName;

    /** 手札枚数 */
    private final int handCount;

    /** 自分の手札 */
    private final List<CardResponse> hand;

    /** パス済みか */
    private final boolean passed;

    /** 確定順位 */
    private final Integer rank;

    /** 自分自身か */
    private final boolean self;

    /** 野獣ルール状態。全プレイヤー共有情報。 */
    private final String yajuStatus;

    /**
     * プレイヤーレスポンスを生成する。
     *
     * @param playerId プレイヤーID
     * @param playerName プレイヤー名
     * @param handCount 手札枚数
     * @param hand 手札
     * @param passed パス済みの場合true
     * @param rank 確定順位
     * @param self 自分自身の場合true
     */
    public PlayerResponse(
            String playerId,
            String playerName,
            int handCount,
            List<CardResponse> hand,
            boolean passed,
            Integer rank,
            boolean self,
            String yajuStatus) {

        this.playerId = playerId;
        this.playerName = playerName;
        this.handCount = handCount;
        this.hand = hand;
        this.passed = passed;
        this.rank = rank;
        this.self = self;
        this.yajuStatus = yajuStatus;
    }

    /**
     * PlayerからレスポンスDTOを生成する。
     *
     * 自分以外の手札内容はクライアントへ返さない。
     *
     * @param player 変換対象のプレイヤー
     * @param requestPlayerId 通信を行ったプレイヤーID
     * @return プレイヤーレスポンス
     */
    public static PlayerResponse from(
            Player player,
            String requestPlayerId) {

        if (player == null) {
            throw new IllegalArgumentException(
                    "プレイヤーはnullにできません。");
        }

        boolean self =
                player.getId().equals(requestPlayerId);

        List<CardResponse> hand;

        if (self) {
            hand = player.getHand()
                    .stream()
                    .map(CardResponse::from)
                    .toList();
        } else {
            hand = Collections.emptyList();
        }

        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getHand().size(),
                hand,
                player.isPassed(),
                player.getRank(),
                self,
                player.getYajuStatus().name());
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getHandCount() {
        return handCount;
    }

    public List<CardResponse> getHand() {
        return hand;
    }

    public boolean isPassed() {
        return passed;
    }

    public Integer getRank() {
        return rank;
    }

    public boolean isSelf() {
        return self;
    }

    public String getYajuStatus() {
        return yajuStatus;
    }
}