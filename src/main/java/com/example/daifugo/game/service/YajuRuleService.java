package com.example.daifugo.game.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameEventType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.domain.YajuStatus;

/**
 * ローカルオリジナル「野獣ルール」の判定を担当するサービス。
 *
 * ルール概要:
 * ・手札に8と10が揃うと野獣ルール対象となる。
 * ・対象化はゲーム開始時だけでなく、7渡しで受け取った結果でも発生する。
 * ・一度対象になったプレイヤーはゲーム中に解除されない。
 * ・対象者は8と10を最低1枚ずつ最後まで保持する。
 * ・余分な8/10は通常プレイおよび7渡しに使用できる。
 * ・最終局面が「8×1 + 10×N」の場合、8を単体で出した後に残りの10を全てまとめて出すと野獣上がり成功。
 * ・上記以外の方法で上がった場合は反則上がりとなる。
 */
public class YajuRuleService {

    /**
     * 配牌直後の全プレイヤーを判定し、8と10を持つプレイヤーを対象化する。
     * 対象化イベントは全端末共有イベントとしてGameStateへ記録する。
     *
     * @param state ゲーム状態
     */
    public void initializeTargets(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        state.getPlayers().forEach(player -> activateIfEligible(state, player));
    }

    /**
     * 現在の手札に8と10が揃っている場合、未対象プレイヤーを野獣対象化する。
     * 7渡し受領後にも呼び出す。
     *
     * @param state ゲーム状態
     * @param player 判定対象プレイヤー
     * @return 今回新規対象化された場合true
     */
    public boolean activateIfEligible(GameState state, Player player) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(player, "player must not be null");

        if (player.isYajuTarget()) {
            return false;
        }

        if (countRank(player.getHand(), Rank.EIGHT) >= 1
                && countRank(player.getHand(), Rank.TEN) >= 1) {
            player.activateYaju();
            state.emitEvent(GameEventType.YAJU_AVAILABLE, player);
            return true;
        }

        return false;
    }

    /**
     * カード提出前に野獣ルール制約を検証する。
     *
     * 通常進行中は最後の1枚の8/10を消費できない。
     * ただし手札が「8×1 + 10×N（N>=1）」だけになった場合は、
     * 8を単体提出して野獣上がりの第1段階へ進める。
     * その後、残った10を全てまとめて提出して上がれば成功となる。
     *
     * 上がりそのものが野獣手順違反の場合はプレイを拒否せず、
     * penaltyFinish=trueを返して最下位判定へ繋げる。
     *
     * @param player プレイヤー
     * @param selectedCards 提出予定カード
     * @return 野獣ルール判定結果
     */
    public YajuPlayDecision validatePlay(
            Player player,
            List<Card> selectedCards
    ) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(selectedCards, "selectedCards must not be null");

        YajuStatus status = player.getYajuStatus();
        if (status == YajuStatus.NONE
                || status == YajuStatus.COMPLETED
                || status == YajuStatus.PENALTY) {
            return YajuPlayDecision.none();
        }

        int handCount = player.getCardCount();
        boolean emptiesHand = selectedCards.size() == handCount;

        if (status == YajuStatus.EIGHT_PLAYED) {
            boolean validTenFinish = emptiesHand
                    && handCount >= 1
                    && areAllRank(selectedCards, Rank.TEN);

            if (validTenFinish) {
                return new YajuPlayDecision(false, true, false);
            }

            if (emptiesHand) {
                return new YajuPlayDecision(false, false, true);
            }

            throw new IllegalArgumentException(
                "野獣上がり中です。8の次は10を出してください"
            );
        }

        /*
         * ACTIVE状態で手札を全て出す場合、8→10を経由していないため
         * 野獣上がり違反としてプレイ自体は許可し、最下位判定にする。
         */
        if (emptiesHand) {
            return new YajuPlayDecision(false, false, true);
        }

        boolean startsEightStep = countRank(player.getHand(), Rank.EIGHT) == 1
                && countRank(player.getHand(), Rank.TEN) >= 1
                && handCount == countRank(player.getHand(), Rank.EIGHT)
                        + countRank(player.getHand(), Rank.TEN)
                && isSingleRank(selectedCards, Rank.EIGHT);

        if (startsEightStep) {
            return new YajuPlayDecision(true, false, false);
        }

        long remainingEight = countRank(player.getHand(), Rank.EIGHT)
                - countRank(selectedCards, Rank.EIGHT);
        long remainingTen = countRank(player.getHand(), Rank.TEN)
                - countRank(selectedCards, Rank.TEN);

        if (remainingEight < 1 || remainingTen < 1) {
            throw new IllegalArgumentException(
                "野獣ルール対象者は8と10を最低1枚ずつ最後まで残してください"
            );
        }

        return YajuPlayDecision.none();
    }

    /**
     * 7渡しで選んだカードを渡しても、野獣対象者が8/10を最低1枚ずつ
     * 保持できることを検証する。
     *
     * @param sourcePlayer 渡す側プレイヤー
     * @param transferCards 渡すカード
     */
    public void validateSevenTransfer(
            Player sourcePlayer,
            List<Card> transferCards
    ) {
        Objects.requireNonNull(sourcePlayer, "sourcePlayer must not be null");
        Objects.requireNonNull(transferCards, "transferCards must not be null");

        if (sourcePlayer.getYajuStatus() != YajuStatus.ACTIVE) {
            return;
        }

        long remainingEight = countRank(sourcePlayer.getHand(), Rank.EIGHT)
                - countRank(transferCards, Rank.EIGHT);
        long remainingTen = countRank(sourcePlayer.getHand(), Rank.TEN)
                - countRank(transferCards, Rank.TEN);

        if (remainingEight < 1 || remainingTen < 1) {
            throw new IllegalArgumentException(
                "野獣ルールは手放せません。8と10を最低1枚ずつ手元に残してください"
            );
        }
    }

    /**
     * 7渡しで実際に渡せる最大枚数を計算する。
     * 野獣対象者は8と10を最低1枚ずつ残すため、その2枚は渡せない。
     *
     * @param player プレイヤー
     * @return 最大譲渡可能枚数
     */
    public int getTransferableCardCount(Player player) {
        Objects.requireNonNull(player, "player must not be null");

        if (player.getYajuStatus() == YajuStatus.ACTIVE) {
            return Math.max(0, player.getCardCount() - 2);
        }

        return player.getCardCount();
    }

    /** 野獣上がり第1段階を確定する。 */
    public void applyEightStep(Player player) {
        player.markYajuEightPlayed();
    }

    /**
     * 野獣上がり成功を確定し、全体共有イベントを発行する。
     */
    public void completeYajuFinish(GameState state, Player player) {
        player.markYajuCompleted();
        state.emitEvent(GameEventType.YAJU_SUCCESS, player);
    }

    /** 野獣上がり違反を確定する。 */
    public void applyPenalty(Player player) {
        player.markYajuPenalty();
    }

    private boolean isSingleRank(List<Card> cards, Rank rank) {
        return cards.size() == 1 && cards.get(0).getRank() == rank;
    }

    private boolean areAllRank(List<Card> cards, Rank rank) {
        return !cards.isEmpty() && cards.stream().allMatch(card -> card.getRank() == rank);
    }

    private long countRank(Collection<Card> cards, Rank rank) {
        return cards.stream()
            .filter(card -> card.getRank() == rank)
            .count();
    }
}
