package com.example.daifugo.response;

import java.util.List;

import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.room.GameRoom;

/**
 * ゲーム内部の状態をクライアント向けレスポンスへ変換するクラス。
 */
public class GameStateResponseMapper {

    /**
     * GameRoomをクライアント向けレスポンスへ変換する。
     *
     * @param room 変換対象の対戦部屋
     * @param requestPlayerId 通信を行ったプレイヤーID
     * @return ゲーム状態レスポンス
     */
    public GameStateResponse map(
            GameRoom room,
            String requestPlayerId) {

        if (room == null) {
            throw new IllegalArgumentException(
                    "対戦部屋はnullにできません。");
        }

        /*
         * ゲーム開始前はGameEngineが存在しないため、
         * 待機中の部屋情報だけを返す。
         */
        if (!room.isStarted()) {

            List<PlayerResponse> players =
                    createPlayerResponses(
                            room.getPlayers(),
                            requestPlayerId);

            return new GameStateResponse(
                    room.getRoomId(),
                    players,
                    null,
                    FieldResponse.from(null),
                    false,
                    false,
                    false);
        }

        GameState gameState =
                room.getGameState();

        List<PlayerResponse> players =
                createPlayerResponses(
                        gameState.getPlayers(),
                        requestPlayerId);

        Player currentPlayer =
                gameState.getPlayers()
                        .get(gameState.getCurrentPlayerIndex());

        String currentPlayerId =
                currentPlayer == null
                        ? null
                        : currentPlayer.getId();

        return new GameStateResponse(
                room.getRoomId(),
                players,
                currentPlayerId,
                FieldResponse.from(
                        gameState.getFieldCombination()),
                gameState.isRevolution(),
                true,
                gameState.isFinished());
    }

    /**
     * プレイヤー一覧をレスポンス一覧へ変換する。
     *
     * @param players プレイヤー一覧
     * @param requestPlayerId 通信を行ったプレイヤーID
     * @return プレイヤーレスポンス一覧
     */
    private List<PlayerResponse> createPlayerResponses(
            List<Player> players,
            String requestPlayerId) {

        return players.stream()
                .map(player ->
                        PlayerResponse.from(
                                player,
                                requestPlayerId))
                .toList();
    }
}