package com.example.daifugo.game.service;

import java.util.List;
import java.util.Objects;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.Deck;
import com.example.daifugo.game.domain.GamePhase;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;


/**
 * ゲーム開始前の初期化処理を担当するクラス。
 *
 * デッキの生成、シャッフル、カード配布、
 * 手札の並び替え、開始プレイヤーの決定を行う。
 */
public class GameInitializer {
    private static final Card START_CARD =
            new Card(Mark.DIAMOND, Rank.THREE);

    private final int jokerCount;

    public GameInitializer(int jokerCount) {
    	if (jokerCount < 0 || jokerCount > 2) {
    		throw new IllegalArgumentException(
    			"ジョーカーの枚数は0～2枚で指定してください"
            );
        }

        this.jokerCount = jokerCount;
    }

    /**
     * ゲーム状態を初期化する。
     *
     * 指定されたジョーカー枚数でデッキを生成し、
     * 各プレイヤーへカードを配布する。
     * 配布後、ダイヤの3を持つプレイヤーを
     * 最初の手番として設定してゲームを開始する。
     *
     * @param state 初期化対象のゲーム状態
     * @throws NullPointerException stateがnullの場合
     * @throws IllegalStateException 待機中でない場合、
     *         または既に手札が配布されている場合
     */
    public void initialize(GameState state) {
        Objects.requireNonNull(
            state,
            "state must not be null"
        );

        validateState(state);
        validateEmptyHands(state);

        Deck deck = Deck.createStandardDeck(jokerCount);
        deck.shuffle();

        List<List<Card>> dealtCards =
            deck.deal(state.getPlayers().size());

        distributeCards(
            state.getPlayers(),
            dealtCards
        );
            
        for (Player player : state.getPlayers()) {
             player.sortHand();
        }
        int firstPlayerIndex =
            findFirstPlayerIndex(state.getPlayers());

        state.changeCurrentPlayer(firstPlayerIndex);
        state.start();
    }

    private void distributeCards(
        List<Player> players,
        List<List<Card>> dealtCards
    ) {
        if (players.size() != dealtCards.size()) {
            throw new IllegalStateException(
                "プレイヤー数と配布先の数が一致しません"
            );
        }

        for (int i = 0; i < players.size(); i++) {
            players.get(i).addCards(dealtCards.get(i));
        }
    }

    private int findFirstPlayerIndex(List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getHand().contains(START_CARD)) {
                return i;
            }
        }

        throw new IllegalStateException(
            "ダイヤの3を持つプレイヤーが存在しません"
        );
    }

    private void validateState(GameState state) {
        if (state.getPhase() != GamePhase.WAITING) {
            throw new IllegalStateException(
                "待機中のゲームのみ初期化できます"
            );
        }
    }

    private void validateEmptyHands(GameState state) {
        boolean hasCard = state.getPlayers().stream()
            .anyMatch(player -> !player.hasNoCards());

        if (hasCard) {
            throw new IllegalStateException(
                "既に手札が配布されています"
            );
        }
    }
}
