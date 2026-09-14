package com.example.daifugo.game.service;

import java.util.ArrayList;
import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.rule.EightCutRule;
import com.example.daifugo.game.rule.MarkLockRule;
import com.example.daifugo.game.rule.PlayValidator;
import com.example.daifugo.game.rule.RevolutionRule;
import com.example.daifugo.game.rule.Rule;
import com.example.daifugo.game.rule.RuleEngine;
import com.example.daifugo.game.rule.finish.EightFinishRule;
import com.example.daifugo.game.rule.finish.FinishValidator;
import com.example.daifugo.game.rule.finish.JokerFinishRule;
import com.example.daifugo.game.rule.finish.SpadeThreeFinishRule;
import com.example.daifugo.game.rule.finish.TwoFinishRule;
import com.example.daifugo.game.rule.FinishRule;

/**
 * ゲーム進行処理を生成するファクトリー。
 */
public class GameEngineFactory {

    /** 現行マルチプレイ互換の標準ルールで生成する。 */
    public GameEngine create() {
        return create(GameRuleSettings.standard());
    }

    /**
     * 指定されたルール設定でゲームエンジンを生成する。
     *
     * @param settings 対戦ルール
     * @return ゲームエンジン
     */
    public GameEngine create(GameRuleSettings settings) {
        PlayValidator playValidator = new PlayValidator();
        TurnManager turnManager = new TurnManager();

        List<Rule> normalRules = new ArrayList<>();
        if (settings.revolution()) {
            normalRules.add(new RevolutionRule());
        }
        if (settings.eightCut()) {
            normalRules.add(new EightCutRule());
        }
        if (settings.markLock()) {
            normalRules.add(new MarkLockRule());
        }

        List<FinishRule> finishRules = new ArrayList<>();
        if (settings.forbiddenFinish()) {
            finishRules.add(new TwoFinishRule());
            finishRules.add(new JokerFinishRule());
            finishRules.add(new EightFinishRule());
            finishRules.add(new SpadeThreeFinishRule());
        }

        return new GameEngine(
                playValidator,
                turnManager,
                new RuleEngine(normalRules),
                new FinishValidator(finishRules),
                new YajuRuleService(),
                settings
        );
    }
}
