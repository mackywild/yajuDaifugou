package com.example.daifugo.console;

import java.util.Arrays;

import com.example.daifugo.game.rule.EightCutRule;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RevolutionRule;
import com.example.daifugo.game.rule.RuleEngine;
import com.example.daifugo.game.rule.finish.FinishValidator;
import com.example.daifugo.game.rule.finish.JokerFinishRule;
import com.example.daifugo.game.rule.finish.TwoFinishRule;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.GameInitializer;
import com.example.daifugo.game.service.TurnManager;

public class ConsoleGameApplication {
    public static void main(String[] args) {
        PlayValidator playValidator =
            new PlayValidator();

        TurnManager turnManager =
            new TurnManager();

        RuleEngine ruleEngine =
            new RuleEngine(
                Arrays.asList(
                    new RevolutionRule(),
                    new EightCutRule()
                )
            );

        FinishValidator finishValidator =
            new FinishValidator(
                Arrays.asList(
                    new TwoFinishRule(),
                    new JokerFinishRule()
                )
            );

        GameEngine gameEngine =
            new GameEngine(
                playValidator,
                turnManager,
                ruleEngine,
                finishValidator
            );

        GameInitializer gameInitializer =
            new GameInitializer(0);

        ConsoleGame consoleGame =
            new ConsoleGame(
                gameInitializer,
                gameEngine
            );

        consoleGame.start();
    }
}
