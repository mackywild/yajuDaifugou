package com.example.daifugo.room;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.mode.GameMode;
import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.GameEngineFactory;
import com.example.daifugo.game.service.GameInitializer;
import com.example.daifugo.game.service.YajuRuleService;

/**
 * 大富豪の対戦部屋を管理するサービス。
 *
 * 部屋の作成、取得、参加、退出、ゲーム開始、ゲーム操作および
 * 一定時間利用されていない部屋の掃除を担当する。
 */
@Service
public class GameRoomService {

    /** ゲームを開始するために必要な最小プレイヤー人数 */
    private static final int MIN_PLAYER_COUNT = 2;

    /** 部屋IDをキーとして対戦部屋を保持する */
    private final Map<String, GameRoom> roomMap = new ConcurrentHashMap<>();

    /** ゲームエンジン生成処理 */
    private final GameEngineFactory gameEngineFactory = new GameEngineFactory();

    /** 野獣ルール初期判定処理 */
    private final YajuRuleService yajuRuleService = new YajuRuleService();

    /** 無操作部屋を削除するまでの時間 */
    @Value("${daifugo.room-ttl-ms:1800000}")
    private long roomTtlMs;

    /**
     * 新しい対戦部屋を作成する。
     *
     * @return 作成した対戦部屋
     */
    public GameRoom createRoom() {
        return createRoom(GameMode.MULTIPLAYER, GameRuleSettings.standard());
    }

    /**
     * ゲームモードとルールを指定して部屋を作成する。
     */
    public GameRoom createRoom(GameMode mode, GameRuleSettings settings) {
        String roomId = createRoomId();
        GameRoom room = new GameRoom(roomId, mode, settings);
        roomMap.put(roomId, room);
        return room;
    }

    /**
     * 指定された部屋を取得し、最終アクセス日時を更新する。
     *
     * @param roomId 部屋ID
     * @return 対戦部屋
     * @throws IllegalArgumentException 部屋が存在しない場合
     */
    public GameRoom getRoom(String roomId) {
        GameRoom room = roomMap.get(roomId);

        if (room == null) {
            throw new IllegalArgumentException(
                    "指定された部屋が存在しません。roomId=" + roomId);
        }

        room.touch();
        return room;
    }

    /**
     * 指定された部屋へプレイヤーを参加させる。
     * 最初に参加したプレイヤーはホストになる。
     *
     * @param roomId 部屋ID
     * @param player 参加するプレイヤー
     * @return 参加後の対戦部屋
     */
    public GameRoom joinRoom(String roomId, Player player) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            room.addPlayer(player);
            return room;
        }
    }

    /**
     * 指定されたプレイヤーを部屋から退出させる。
     * 退出後に参加者が0人になった場合は部屋自体も削除する。
     *
     * @param roomId 部屋ID
     * @param playerId 退出するプレイヤーID
     */
    public void leaveRoom(String roomId, String playerId) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            boolean removed = room.removePlayer(playerId);

            if (!removed) {
                throw new IllegalArgumentException(
                        "指定されたプレイヤーは部屋に参加していません。playerId=" + playerId);
            }

            if (room.getPlayerCount() == 0) {
                roomMap.remove(roomId, room);
            }
        }
    }

    /**
     * 指定された部屋を削除する。
     *
     * @param roomId 部屋ID
     */
    public void removeRoom(String roomId) {
        roomMap.remove(roomId);
    }

    /**
     * 現在存在する部屋一覧を取得する。
     *
     * @return 読み取り専用の対戦部屋一覧
     */
    public Collection<GameRoom> getRooms() {
        return Collections.unmodifiableCollection(roomMap.values());
    }

    /**
     * 指定された部屋のゲームを開始する。
     *
     * @param roomId ゲームを開始する部屋ID
     * @return 開始後の対戦部屋
     */
    public GameRoom startGame(String roomId) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            if (room.isStarted()) {
                throw new IllegalStateException("ゲームは既に開始されています");
            }

            if (room.getPlayerCount() < MIN_PLAYER_COUNT) {
                throw new IllegalStateException(
                        "ゲーム開始には最低" + MIN_PLAYER_COUNT
                        + "人必要です。現在の参加人数=" + room.getPlayerCount());
            }

            List<Player> players = new ArrayList<>(room.getPlayers());
            GameState gameState = new GameState(players);
            GameRuleSettings settings = room.getRuleSettings();
            new GameInitializer(settings.jokerCount()).initialize(gameState);

            /*
             * 野獣ルールが有効な場合のみ、配牌時点で8と10を持っている
             * プレイヤーを対象化する。
             */
            if (settings.yajuRule()) {
                yajuRuleService.initializeTargets(gameState);
            }

            GameEngine gameEngine = gameEngineFactory.create(settings);
            room.start(gameState, gameEngine);
            return room;
        }
    }

    /**
     * 指定されたプレイヤーのカード提出を処理する。
     *
     * @param roomId 部屋ID
     * @param playerId プレイヤーID
     * @param cards 提出カード
     * @return 更新後の部屋
     */
    public GameRoom play(String roomId, String playerId, List<Card> cards) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            ensureStarted(room);
            room.getGameEngine().play(room.getGameState(), playerId, cards);
            room.touch();
            return room;
        }
    }

    /**
     * 指定されたプレイヤーのパスを処理する。
     *
     * @param roomId 部屋ID
     * @param playerId プレイヤーID
     * @return 更新後の部屋
     */
    public GameRoom pass(String roomId, String playerId) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            ensureStarted(room);
            room.getGameEngine().pass(room.getGameState(), playerId);
            room.touch();
            return room;
        }
    }

    /**
     * 保留中の7渡しを実行する。
     *
     * @param roomId 部屋ID
     * @param playerId 渡す側プレイヤーID
     * @param cards 渡すカード
     * @return 更新後の部屋
     */
    public GameRoom transferSeven(
            String roomId,
            String playerId,
            List<Card> cards
    ) {
        GameRoom room = getRoom(roomId);

        synchronized (room) {
            ensureStarted(room);
            room.getGameEngine().transferSeven(
                room.getGameState(),
                playerId,
                cards
            );
            room.touch();
            return room;
        }
    }

    /**
     * 一定時間アクセスされていない部屋を削除する。
     * ポーリング中の部屋はgetRoomでtouchされるため削除対象にならない。
     */
    @Scheduled(fixedDelayString = "${daifugo.room-cleanup-interval-ms:300000}")
    public void cleanupExpiredRooms() {
        long expireBefore = System.currentTimeMillis() - roomTtlMs;
        roomMap.entrySet().removeIf(entry ->
                entry.getValue().getLastAccessedAt() < expireBefore);
    }

    /**
     * 新しい部屋IDを生成する。
     *
     * @return 8文字の部屋ID
     */
    private String createRoomId() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }

    /**
     * ゲーム開始済みであることを確認する。
     *
     * @param room 対戦部屋
     */
    private void ensureStarted(GameRoom room) {
        if (!room.isStarted() || room.getGameState() == null || room.getGameEngine() == null) {
            throw new IllegalStateException("ゲームが開始されていません");
        }
    }
}
