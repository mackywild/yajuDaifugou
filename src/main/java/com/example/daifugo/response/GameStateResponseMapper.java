package com.example.daifugo.response;

import java.util.List;

import com.example.daifugo.game.cpu.CpuMove;
import com.example.daifugo.game.cpu.LegalMoveGenerator;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.room.GameRoom;

/**
 * ゲーム内部の状態をクライアント向けレスポンスへ変換するクラス。
 */
public class GameStateResponseMapper {

    private final LegalMoveGenerator legalMoveGenerator = new LegalMoveGenerator();

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
                    room.getGameMode().name(),
                    RuleSettingsResponse.from(room.getRuleSettings()),
                    players,
                    null,
                    FieldResponse.from(null),
                    false,
                    false,
                    null,
                    room.isHost(requestPlayerId),
                    false,
                    false,
                    SevenTransferResponse.none(),
                    List.of(),
                    List.of());
        }

        GameState gameState =
                room.getGameState();

        /*
         * ゲーム終了後は退出済みプレイヤーを表示し続けないよう、
         * 現在GameRoomに残っている参加者をレスポンス対象にする。
         */
        List<Player> responsePlayers = gameState.isFinished()
                ? room.getPlayers()
                : gameState.getPlayers();

        List<PlayerResponse> players =
                createPlayerResponses(
                        responsePlayers,
                        requestPlayerId);

        String currentPlayerId = null;
        if (!gameState.isFinished()) {
            Player currentPlayer =
                    gameState.getPlayers()
                            .get(gameState.getCurrentPlayerIndex());
            currentPlayerId = currentPlayer.getId();
        }

        List<List<CardResponse>> legalMoves = createLegalMoves(
                room,
                gameState,
                requestPlayerId);

        return new GameStateResponse(
                room.getRoomId(),
                room.getGameMode().name(),
                RuleSettingsResponse.from(room.getRuleSettings()),
                players,
                currentPlayerId,
                FieldResponse.from(
                        gameState.getFieldCombination()),
                gameState.isRevolution(),
                gameState.isJackBack(),
                gameState.getLockedMark() == null ? null : gameState.getLockedMark().name(),
                room.isHost(requestPlayerId),
                true,
                gameState.isFinished(),
                SevenTransferResponse.from(gameState.getPendingSevenTransfer()),
                gameState.getEvents().stream()
                        .map(GameEventResponse::from)
                        .toList(),
                legalMoves);
    }

    private List<List<CardResponse>> createLegalMoves(
            GameRoom room,
            GameState gameState,
            String requestPlayerId
    ) {
        if (gameState.isFinished()
                || !gameState.getCurrentPlayer().getId().equals(requestPlayerId)) {
            return List.of();
        }

        Player player = gameState.getCurrentPlayer();

        if (gameState.hasPendingSevenTransfer()) {
            if (!gameState.getPendingSevenTransfer().sourcePlayerId().equals(requestPlayerId)) {
                return List.of();
            }
            return legalMoveGenerator.generateSevenTransfers(
                            player,
                            gameState.getPendingSevenTransfer().cardCount(),
                            room.getRuleSettings())
                    .stream()
                    .map(cards -> cards.stream().map(CardResponse::from).toList())
                    .toList();
        }

        return legalMoveGenerator.generate(
                        gameState,
                        player,
                        room.getRuleSettings())
                .stream()
                .filter(move -> !move.pass())
                .map(CpuMove::cards)
                .map(cards -> cards.stream().map(CardResponse::from).toList())
                .toList();
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