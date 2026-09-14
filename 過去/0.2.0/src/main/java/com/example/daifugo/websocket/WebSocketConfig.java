package com.example.daifugo.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketおよびSTOMP通信の設定クラス。
 *
 * クライアントが接続するWebSocketエンドポイントと、
 * メッセージの送受信に使用する宛先プレフィックスを設定する。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    /**
     * メッセージブローカーを設定する。
     *
     * /appから始まる宛先はControllerの
     * {@code @MessageMapping}メソッドへ転送する。
     *
     * /topicから始まる宛先は、
     * 購読している複数のクライアントへ配信する。
     *
     * @param registry メッセージブローカー設定
     */
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        /*
         * クライアントからサーバーへ送信する際に使用する
         * 宛先の共通プレフィックスを設定する。
         */
        registry.setApplicationDestinationPrefixes("/app");

        /*
         * サーバーから複数クライアントへ配信するための
         * インメモリメッセージブローカーを有効化する。
         */
        registry.enableSimpleBroker("/topic", "/queue");
    }

    /**
     * WebSocket接続用のエンドポイントを登録する。
     *
     * Vue側は /ws-game に接続して、
     * STOMP通信を開始する。
     *
     * @param registry STOMPエンドポイント設定
     */
    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.addEndpoint("/ws-game")
                /*
                 * Vue開発サーバーからの接続を許可する。
                 *
                 * 開発段階ではlocalhostのみに限定する。
                 */
                .setAllowedOriginPatterns(
                        "http://localhost:*");
    }
}