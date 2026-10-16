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
    void 階段は場の連番と重なる組み合わせでは返せない() {
        CardCombination field = straight(
            Rank.FIVE,
            Rank.SIX,
            Rank.SEVEN
        );

        assertFalse(
            validator.canPlay(
                straight(Rank.SIX, Rank.SEVEN, Rank.EIGHT),
                field,
                false,
                null
            )
        );

        assertFalse(
            validator.canPlay(
                straight(Rank.SEVEN, Rank.EIGHT, Rank.NINE),
                field,
                false,
                null
            )
        );
    }

    @Test
    void 五六七の階段に出せる最弱は八九十() {
        CardCombination field = straight(
            Rank.FIVE,
            Rank.SIX,
            Rank.SEVEN
        );

        assertTrue(
            validator.canPlay(
                straight(Rank.EIGHT, Rank.NINE, Rank.TEN),
                field,
                false,
                null
            )
        );
    }

    @Test
    void 革命中の階段も場の範囲をまたいでは出せない() {
        CardCombination field = straight(
            Rank.EIGHT,
            Rank.NINE,
            Rank.TEN
        );

        assertFalse(
            validator.canPlay(
                straight(Rank.SEVEN, Rank.EIGHT, Rank.NINE),
                field,
                true,
                null
            )
        );

        assertTrue(
            validator.canPlay(
                straight(Rank.FIVE, Rank.SIX, Rank.SEVEN),
                field,
                true,
                null
            )
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

    private CardCombination straight(Rank... ranks) {
        return CardCombination.of(
            java.util.Arrays.stream(ranks)
                .map(rank -> new Card(Mark.SPADE, rank))
                .toList()
        );
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
