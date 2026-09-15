package com.example.daifugo.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.GameInitializer;

public class ConsoleGame {
    private final Scanner scanner;
    private final GameInitializer gameInitializer;
    private final GameEngine gameEngine;

    public ConsoleGame(
            GameInitializer gameInitializer,
            GameEngine gameEngine
    ) {
        this.scanner = new Scanner(System.in);
        this.gameInitializer = gameInitializer;
        this.gameEngine = gameEngine;
    }

    public void start() {
        List<Player> players = createPlayers();

        GameState state = new GameState(players);

        gameInitializer.initialize(state);

        System.out.println("=== 大富豪ゲーム開始 ===");

        while (state.getPhase().isPlaying()) {
            printGameState(state);

            Player currentPlayer =
                state.getCurrentPlayer();

            executeTurn(state, currentPlayer);
        }

        printResult(state);
    }

    private List<Player> createPlayers() {
        return Arrays.asList(
            new Player("p1", "プレイヤー1"),
            new Player("p2", "プレイヤー2")
        );
    }

    private void executeTurn(
            GameState state,
            Player player
    ) {
        System.out.println();
        System.out.println(
            player.getName() + "のターン"
        );

        printHand(player);

        System.out.println(
            "出すカード番号を入力してください"
        );
        System.out.println(
            "複数枚はカンマ区切り、パスは p"
        );

        String input =
            scanner.nextLine().trim();

        if ("p".equalsIgnoreCase(input)) {
            tryPass(state, player);
            return;
        }

        tryPlay(state, player, input);
    }

    private void tryPlay(
            GameState state,
            Player player,
            String input
    ) {
        try {
            List<Card> selectedCards =
                parseSelectedCards(
                    player,
                    input
                );

            gameEngine.play(
                state,
                player.getId(),
                selectedCards
            );

            System.out.println(
                player.getName()
                    + "が "
                    + selectedCards
                    + " を出しました"
            );

        } catch (RuntimeException e) {
            System.out.println(
                "出せません: "
                    + e.getMessage()
            );
        }
    }

    private void tryPass(
            GameState state,
            Player player
    ) {
        try {
            gameEngine.pass(
                state,
                player.getId()
            );

            System.out.println(
                player.getName()
                    + "はパスしました"
            );

        } catch (RuntimeException e) {
            System.out.println(
                "パスできません: "
                    + e.getMessage()
            );
        }
    }

    private List<Card> parseSelectedCards(
            Player player,
            String input
    ) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                "カード番号を入力してください"
            );
        }

        String[] values =
            input.split(",");

        List<Integer> indexes =
            Arrays.stream(values)
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<Card> hand =
            player.getHand();

        List<Card> selectedCards =
            new ArrayList<>();

        for (Integer index : indexes) {
            if (index == null
                    || index < 1
                    || index > hand.size()) {
                throw new IllegalArgumentException(
                    "存在しないカード番号です"
                );
            }

            Card card =
                hand.get(index - 1);

            if (selectedCards.contains(card)) {
                throw new IllegalArgumentException(
                    "同じカード番号が重複しています"
                );
            }

            selectedCards.add(card);
        }

        return selectedCards;
    }

    private void printHand(Player player) {
        List<Card> hand =
            player.getHand();

        System.out.println("手札:");

        for (int i = 0; i < hand.size(); i++) {
            System.out.println(
                (i + 1)
                    + ": "
                    + hand.get(i)
            );
        }
    }

    private void printGameState(GameState state) {
        System.out.println();
        System.out.println("--------------------");

        CardCombination field =
            state.getFieldCombination();

        if (field == null) {
            System.out.println("場: なし");
        } else {
            System.out.println(
                "場: " + field.getCards()
            );
        }

        System.out.println(
            "革命: "
                + (state.isRevolution()
                    ? "発生中"
                    : "なし")
        );


        for (Player player : state.getPlayers()) {
            System.out.println(
                player.getName()
                    + " 残り"
                    + player.getCardCount()
                    + "枚"
            );
        }
    }

    private void printResult(GameState state) {
        System.out.println();
        System.out.println("=== ゲーム終了 ===");

        state.getPlayers()
            .stream()
            .sorted((left, right) ->
                Integer.compare(
                    left.getRank(),
                    right.getRank()
                )
            )
            .forEach(player ->
                System.out.println(
                    player.getRank()
                        + "位: "
                        + player.getName()
                )
            );
    }
}
