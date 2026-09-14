package com.example.daifugo.game.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * プレイヤー情報
 */
public class Player {

    private final String id;
    private final String name;

    private final List<Card> hand = new ArrayList<>();

    private boolean passed;
    private Integer rank;

    /** 野獣ルールの進行状態 */
    private YajuStatus yajuStatus = YajuStatus.NONE;

    public Player(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public boolean isPassed() {
        return passed;
    }

    public Integer getRank() {
        return rank;
    }

    public void addCard(Card card) {
        hand.add(Objects.requireNonNull(card, "card must not be null"));
    }

    public void addCards(Collection<Card> cards) {
        Objects.requireNonNull(cards, "cards must not be null");

        if (cards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                "cards must not contain null"
            );
        }

        hand.addAll(cards);
    }

    public void removeCards(Collection<Card> cards) {
        Objects.requireNonNull(cards, "cards must not be null");

        if (cards.isEmpty()) {
            throw new IllegalArgumentException(
                "削除するカードが指定されていません"
            );
        }

        if (!containsAllCards(cards)) {
            throw new IllegalArgumentException(
                "所持していないカードがあります"
            );
        }

        for (Card card : cards) {
            hand.remove(card);
        }
    }

    public void pass() {
        passed = true;
    }

    public void clearPass() {
        passed = false;
    }

    public boolean hasNoCards() {
        return hand.isEmpty();
    }

    public int getCardCount() {
        return hand.size();
    }

    public void assignRank(int rank) {
        if (rank <= 0) {
            throw new IllegalArgumentException(
                "順位は1以上で指定してください"
            );
        }

        if (this.rank != null) {
            throw new IllegalStateException(
                "順位は既に確定しています"
            );
        }

        this.rank = rank;
    }

    public boolean hasFinished() {
        return rank != null;
    }

    /**
     * 野獣ルールの現在状態を取得する。
     *
     * @return 野獣ルール状態
     */
    public YajuStatus getYajuStatus() {
        return yajuStatus;
    }

    /**
     * 一度でも野獣ルール対象になったか判定する。
     * COMPLETED/PENALTYも「対象だった」状態としてtrueを返す。
     *
     * @return 野獣ルール対象の場合true
     */
    public boolean isYajuTarget() {
        return yajuStatus != YajuStatus.NONE;
    }

    /**
     * 野獣ルールを適用する。
     * 一度対象になったプレイヤーはゲーム中にNONEへ戻らない。
     *
     * @return 今回新たに対象になった場合true
     */
    public boolean activateYaju() {
        if (yajuStatus != YajuStatus.NONE) {
            return false;
        }
        yajuStatus = YajuStatus.ACTIVE;
        return true;
    }

    /** 最終8を正しく出し、10待ち状態へ進める。 */
    public void markYajuEightPlayed() {
        if (yajuStatus != YajuStatus.ACTIVE) {
            throw new IllegalStateException("野獣上がりの8を出せる状態ではありません");
        }
        yajuStatus = YajuStatus.EIGHT_PLAYED;
    }

    /** 野獣上がり成功状態へ進める。 */
    public void markYajuCompleted() {
        if (yajuStatus != YajuStatus.EIGHT_PLAYED) {
            throw new IllegalStateException("野獣上がり成功条件を満たしていません");
        }
        yajuStatus = YajuStatus.COMPLETED;
    }

    /** 野獣上がり違反による反則状態へ進める。 */
    public void markYajuPenalty() {
        if (yajuStatus == YajuStatus.NONE) {
            throw new IllegalStateException("野獣ルール対象ではありません");
        }
        yajuStatus = YajuStatus.PENALTY;
    }

    /** 指定ランクのカード所持枚数を返す。 */
    public long countRank(Rank targetRank) {
        Objects.requireNonNull(targetRank, "targetRank must not be null");
        return hand.stream()
            .filter(card -> card.getRank() == targetRank)
            .count();
    }

    private boolean containsAllCards(Collection<Card> cards) {
        List<Card> copiedHand = new ArrayList<>(hand);

        for (Card card : cards) {
            if (!copiedHand.remove(card)) {
                return false;
            }
        }

        return true;
    }
    
    public void sortHand() {
         hand.sort(
             Comparator.comparingInt(
                 card -> card.getStrength(false)
             )
         );
     }
    
}