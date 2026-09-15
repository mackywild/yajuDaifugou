package com.example.daifugo.response;

import com.example.daifugo.game.config.GameRuleSettings;

/** クライアント表示用のルール設定。 */
public record RuleSettingsResponse(
        int jokerCount,
        boolean revolution,
        boolean eightCut,
        boolean markLock,
        boolean sevenTransfer,
        boolean yajuRule,
        boolean jackBack,
        boolean forbiddenFinish
) {
    public static RuleSettingsResponse from(GameRuleSettings settings) {
        return new RuleSettingsResponse(
                settings.jokerCount(),
                settings.revolution(),
                settings.eightCut(),
                settings.markLock(),
                settings.sevenTransfer(),
                settings.yajuRule(),
                settings.jackBack(),
                settings.forbiddenFinish()
        );
    }
}
