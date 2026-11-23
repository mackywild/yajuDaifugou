package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** プレイヤー名の受入条件と生成時の正規化を固定する。 */
class PlayerRegistrationFactoryTest {
    private final PlayerRegistrationFactory factory = new PlayerRegistrationFactory();

    @Test
    void trimsNameAndCreatesDifferentIdentifiers() {
        var first = factory.createPlayer("  テスト  ");
        var second = factory.createPlayer("  テスト  ");
        assertEquals("テスト", first.getName());
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void rejectsInvalidNamesWithoutChangingExistingMessages() {
        assertEquals("プレイヤー名を入力してください",
                assertThrows(IllegalArgumentException.class, () -> factory.validatePlayerName(" ")).getMessage());
        assertEquals("プレイヤー名は20文字以内にしてください",
                assertThrows(IllegalArgumentException.class, () -> factory.validatePlayerName("あ".repeat(21))).getMessage());
    }
}
