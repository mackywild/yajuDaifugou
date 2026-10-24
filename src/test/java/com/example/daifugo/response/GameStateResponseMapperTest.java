package com.example.daifugo.response;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.game.service.GameEngineFactory;
import com.example.daifugo.room.GameRoom;

class GameStateResponseMapperTest {

    private final GameStateResponseMapper mapper = new GameStateResponseMapper();

    @Test
    void 現在手番の本人へ合法手候補を返す() {
        Player self = new Player("p1", "P1");
        self.addCard(new Card(Mark.SPADE, Rank.FIVE));
        Player other = new Player("p2", "P2");
        other.addCard(new Card(Mark.HEART, Rank.SIX));

        GameRoom room = startedRoom(self, other);

        GameStateResponse response = mapper.map(room, self.getId());

        assertTrue(response.getLegalMoves().stream().anyMatch(move ->
                move.size() == 1
                        && move.get(0).getRank().equals(Rank.FIVE.name())
                        && move.get(0).getSuit().equals(Mark.SPADE.name())));
    }

    @Test
    void 手番ではないプレイヤーへ合法手を公開しない() {
        Player current = new Player("p1", "P1");
        current.addCard(new Card(Mark.SPADE, Rank.FIVE));
        Player observer = new Player("p2", "P2");
        observer.addCard(new Card(Mark.HEART, Rank.SIX));

        GameRoom room = startedRoom(current, observer);

        GameStateResponse response = mapper.map(room, observer.getId());

        assertTrue(response.getLegalMoves().isEmpty());
    }

    private static GameRoom startedRoom(Player first, Player second) {
        GameRoom room = new GameRoom("room");
        room.addPlayer(first);
        room.addPlayer(second);

        GameState state = new GameState(List.of(first, second));
        state.start();
        state.changeCurrentPlayer(0);
        room.start(state, new GameEngineFactory().create());

        return room;
    }
}
