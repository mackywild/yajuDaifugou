package com.example.daifugo.game.service;

import java.util.Arrays;

import com.example.daifugo.game.rule.EightCutRule;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RevolutionRule;
import com.example.daifugo.game.rule.MarkLockRule;
import com.example.daifugo.game.rule.RuleEngine;
import com.example.daifugo.game.rule.finish.FinishValidator;
import com.example.daifugo.game.rule.finish.EightFinishRule;
import com.example.daifugo.game.rule.finish.JokerFinishRule;
import com.example.daifugo.game.rule.finish.SpadeThreeFinishRule;
import com.example.daifugo.game.rule.finish.TwoFinishRule;
/**
 * ゲーム進行処理を生成するファクトリー。
 */
public class GameEngineFactory {

    /**
     * ゲームエンジンを生成する。
     *
     * GameEngineはGameStateを保持せず、
     * playまたはpass実行時にGameStateを受け取る。
     *
     * @return ゲームエンジン
     */
    public GameEngine create() {

        PlayValidator playValidator =
                new PlayValidator();

        TurnManager turnManager =
                new TurnManager();

        RuleEngine ruleEngine =
                new RuleEngine(
                        Arrays.asList(
                                new RevolutionRule(),
                                new EightCutRule(),
                                new MarkLockRule()
                        )
                );

        FinishValidator finishValidator =
                new FinishValidator(
                        Arrays.asList(
                                new TwoFinishRule(),
                                new JokerFinishRule(),
                                new EightFinishRule(),
                                new SpadeThreeFinishRule()
                        )
                );

        return new GameEngine(
                playValidator,
                turnManager,
                ruleEngine,
                finishValidator
        );
    }
}