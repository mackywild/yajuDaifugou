package com.example.daifugo.game.rule;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.domain.CardCombination;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Rank;

public class PlayValidatorTest {
    private final PlayValidator validator = new PlayValidator();

    @Test
    void 場が空なら有効なカードを出せる() {
        CardCombination selected = single(Rank.FIVE);

        assertTrue(
            validator.canPlay(selected, null, false, null)
        );
    }

    @Test
    void 場より強いシングルを出せる() {
        CardCombination field = single(Rank.FIVE);
        CardCombination selected = single(Rank.SIX);

        assertTrue(
            validator.canPlay(selected, field, false, null)
        );
    }

    @Test
    void 場より弱いシングルは出せない() {
        CardCombination field = single(Rank.TEN);
        CardCombination selected = single(Rank.SIX);

        assertFalse(
            validator.canPlay(selected, field, false, null)
        );
    }

    @Test
    void 同じ強さのカードは出せない() {
        CardCombination field = single(Rank.SIX);
        CardCombination selected = single(Rank.SIX);

        assertFalse(
            validator.canPlay(selected, field, false, null)
        );
    }

    @Test
    void シングルの場にペアは出せない() {
        CardCombination field = single(Rank.FIVE);

        CardCombination selected = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.SIX),
            new Card(Mark.HEART, Rank.SIX)
        ));

        assertFalse(
            validator.canPlay(selected, field, false, null)
        );
    }

    @Test
    void 革命中は弱い数字が強くなる() {
        CardCombination field = single(Rank.TEN);
        CardCombination selected = single(Rank.FIVE);

        assertTrue(
            validator.canPlay(selected, field, true, null)
        );
    }

    @Test
    void 無効な組み合わせは出せない() {
        CardCombination selected = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FIVE),
            new Card(Mark.HEART, Rank.SIX)
        ));

        assertFalse(
            validator.canPlay(selected, null, false, null)
        );
    }

    @Test
    void JOKER返し成立後のスペード3には他のカードを出せない() {
        CardCombination field = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE)
        ));
        CardCombination selected = single(Rank.TWO);

        assertFalse(
            validator.canPlay(selected, field, false, null, true)
        );
    }

    @Test
    void JOKER返し成立後のスペード3にはJOKERでも返せない() {
        CardCombination field = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE)
        ));
        CardCombination joker = CardCombination.of(List.of(
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertFalse(
            validator.canPlay(joker, field, false, null, true)
        );
    }

    @Test
    void 通常のスペード3にはより強いカードを出せる() {
        CardCombination field = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE)
        ));
        CardCombination selected = single(Rank.FOUR);

        assertTrue(
            validator.canPlay(selected, field, false, null, false)
        );
    }

    private CardCombination single(Rank rank) {
        return CardCombination.of(List.of(
            new Card(Mark.SPADE, rank)
        ));
    }

    @Test
    void Jバック中は3とJOKERのペアを4のペアへ出せる() {
        CardCombination field = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FOUR),
            new Card(Mark.HEART, Rank.FOUR)
        ));
        CardCombination selected = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertTrue(
            validator.canPlay(selected, field, true, null)
        );
    }

    @Test
    void 通常時は4とJOKERのペアを3のペアへ出せる() {
        CardCombination field = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.THREE),
            new Card(Mark.HEART, Rank.THREE)
        ));
        CardCombination selected = CardCombination.of(List.of(
            new Card(Mark.SPADE, Rank.FOUR),
            new Card(Mark.JOKER, Rank.JOKER)
        ));

        assertTrue(
            validator.canPlay(selected, field, false, null)
        );
    }

}
