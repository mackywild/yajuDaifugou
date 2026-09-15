package com.example.daifugo.game.cpu;

import java.util.List;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;

/** CPUの思考ロジック。 */
public interface CpuStrategy {
    CpuMove chooseMove(
            GameState state,
            Player player,
            List<CpuMove> legalMoves,
            GameRuleSettings settings
    );

    List<Card> chooseSevenTransfer(
            GameState state,
            Player player,
            List<List<Card>> legalTransfers,
            GameRuleSettings settings
    );
}
