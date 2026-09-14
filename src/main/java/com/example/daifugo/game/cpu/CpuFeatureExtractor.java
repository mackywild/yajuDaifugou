package com.example.daifugo.game.cpu;

import java.util.ArrayList;
import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.CombinationType;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.domain.YajuStatus;

/** N-GOD学習/推論で共通利用する特徴量抽出器。 */
public final class CpuFeatureExtractor {
    public static final int FEATURE_COUNT = 38;

    private CpuFeatureExtractor() {}

    public static double[] extract(
            GameState state,
            Player player,
            CpuMove move,
            GameRuleSettings settings
    ) {
        double[] f = new double[FEATURE_COUNT];
        List<Player> opponents = state.getPlayers().stream()
                .filter(p -> !p.getId().equals(player.getId()) && !p.hasFinished())
                .toList();
        int minOpp = opponents.stream().mapToInt(Player::getCardCount).min().orElse(0);
        int maxOpp = opponents.stream().mapToInt(Player::getCardCount).max().orElse(0);
        double avgOpp = opponents.stream().mapToInt(Player::getCardCount).average().orElse(0.0);

        List<Card> remaining = new ArrayList<>(player.getHand());
        for (Card card : move.cards()) {
            remaining.remove(card);
        }

        CardCombination combination = move.pass() ? null : CardCombination.of(move.cards());
        int strength = 0;
        if (combination != null && combination.isValid()) {
            if (combination.isSingleJoker()) {
                strength = Rank.JOKER.getStrength();
            } else {
                strength = combination.getBaseStrength();
            }
        }

        boolean willFinish = !move.pass() && remaining.isEmpty();
        boolean forbiddenFinish = willFinish && settings.forbiddenFinish()
                && containsForbiddenFinish(move.cards());
        boolean yajuStart = player.getYajuStatus() == YajuStatus.ACTIVE
                && player.getCardCount() == 2
                && isSingleRank(move.cards(), Rank.EIGHT);
        boolean yajuSuccess = player.getYajuStatus() == YajuStatus.EIGHT_PLAYED
                && player.getCardCount() == 1
                && isSingleRank(move.cards(), Rank.TEN);

        f[0] = player.getCardCount() / 27.0;
        f[1] = remaining.size() / 27.0;
        f[2] = minOpp / 27.0;
        f[3] = avgOpp / 27.0;
        f[4] = maxOpp / 27.0;
        f[5] = state.getFieldCombination() == null ? 0.0 : 1.0;
        f[6] = state.getFieldCombination() == null ? 0.0 : state.getFieldCombination().getCardCount() / 4.0;
        f[7] = state.isRevolution() ? 1.0 : 0.0;
        f[8] = state.getLockedMark() == null ? 0.0 : 1.0;
        f[9] = move.pass() ? 1.0 : 0.0;
        f[10] = move.cards().size() / 4.0;
        f[11] = strength / 16.0;
        f[12] = containsRank(move.cards(), Rank.SEVEN) ? 1.0 : 0.0;
        f[13] = containsRank(move.cards(), Rank.EIGHT) ? 1.0 : 0.0;
        f[14] = containsRank(move.cards(), Rank.TEN) ? 1.0 : 0.0;
        f[15] = containsRank(move.cards(), Rank.TWO) ? 1.0 : 0.0;
        f[16] = move.cards().stream().anyMatch(Card::isJoker) ? 1.0 : 0.0;
        f[17] = combination != null && combination.getType() == CombinationType.PAIR ? 1.0 : 0.0;
        f[18] = combination != null && combination.getType() == CombinationType.TRIPLE ? 1.0 : 0.0;
        f[19] = combination != null && combination.getType() == CombinationType.FOUR ? 1.0 : 0.0;
        f[20] = combination != null && combination.getType() == CombinationType.STRAIGHT ? 1.0 : 0.0;
        f[21] = willFinish ? 1.0 : 0.0;
        f[22] = player.getYajuStatus() == YajuStatus.ACTIVE ? 1.0 : 0.0;
        f[23] = player.getYajuStatus() == YajuStatus.EIGHT_PLAYED ? 1.0 : 0.0;
        f[24] = yajuStart ? 1.0 : 0.0;
        f[25] = yajuSuccess ? 1.0 : 0.0;
        f[26] = minOpp > 0 && minOpp <= 2 ? 1.0 : 0.0;
        f[27] = settings.eightCut() && containsRank(move.cards(), Rank.EIGHT) ? 1.0 : 0.0;
        f[28] = settings.sevenTransfer() && containsRank(move.cards(), Rank.SEVEN) ? 1.0 : 0.0;
        f[29] = fragmentation(remaining);
        f[30] = ratioByStrength(remaining, false);
        f[31] = ratioByStrength(remaining, true);
        f[32] = settings.revolution() ? 1.0 : 0.0;
        f[33] = settings.eightCut() ? 1.0 : 0.0;
        f[34] = settings.markLock() ? 1.0 : 0.0;
        f[35] = settings.sevenTransfer() ? 1.0 : 0.0;
        f[36] = settings.yajuRule() ? 1.0 : 0.0;
        f[37] = forbiddenFinish ? 1.0 : 0.0;
        return f;
    }

    private static double fragmentation(List<Card> cards) {
        if (cards.isEmpty()) {
            return 0.0;
        }
        long distinctRanks = cards.stream().map(Card::getRank).distinct().count();
        return distinctRanks / (double) cards.size();
    }

    /** 弱い/強いカードの比率。 */
    private static double ratioByStrength(List<Card> cards, boolean high) {
        if (cards.isEmpty()) return 0.0;
        long count = cards.stream().filter(card -> {
            int s = card.getRank().getStrength();
            return high ? s >= Rank.KING.getStrength() : s <= Rank.EIGHT.getStrength();
        }).count();
        return count / (double) cards.size();
    }

    public static boolean containsForbiddenFinish(List<Card> cards) {
        return cards.stream().anyMatch(card ->
                card.getRank() == Rank.TWO
                || card.getRank() == Rank.EIGHT
                || card.isJoker()
                || (card.getSuit() == Mark.SPADE && card.getRank() == Rank.THREE));
    }

    public static boolean containsRank(List<Card> cards, Rank rank) {
        return cards.stream().anyMatch(card -> card.getRank() == rank);
    }

    public static boolean isSingleRank(List<Card> cards, Rank rank) {
        return cards.size() == 1 && cards.get(0).getRank() == rank;
    }
}
