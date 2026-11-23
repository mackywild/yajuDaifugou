package com.example.daifugo.web;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.example.daifugo.game.domain.Player;

/** REST入力のプレイヤー名検証とプレイヤー生成を受け持つ。 */
@Component
public class PlayerRegistrationFactory {
    public Player createPlayer(String playerName) {
        return new Player(
                UUID.randomUUID().toString(),
                playerName.trim());
    }

    public void validatePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("プレイヤー名を入力してください");
        }
        if (playerName.length() > 20) {
            throw new IllegalArgumentException("プレイヤー名は20文字以内にしてください");
        }
    }

}
