package com.example.daifugo.room;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.daifugo.game.domain.GameState;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.service.GameEngine;
import com.example.daifugo.game.service.GameEngineFactory;
import com.example.daifugo.game.service.GameInitializer;

/**
 * 大富豪の対戦部屋を管理するサービス。
 *
 * 部屋の作成、取得、参加、退出、削除を担当する。
 */
@Service
public class GameRoomService {

    /** 部屋IDをキーとして対戦部屋を保持する */
    private final Map<String, GameRoom> roomMap;
    
    /** ゲームを開始するために必要なプレイヤー人数 */
    private static final int REQUIRED_PLAYER_COUNT = 4;

    /** ゲーム初期化処理 */
    private final GameInitializer gameInitializer;

    private final GameEngineFactory gameEngineFactory;
    /**
     * 部屋管理サービスを生成する。
     */
    public GameRoomService() {
        this.roomMap = new ConcurrentHashMap<>();
        this.gameInitializer = new GameInitializer(1);
        this.gameEngineFactory = new GameEngineFactory();
    }

    /**
     * 新しい対戦部屋を作成する。
     *
     * @return 作成した対戦部屋
     */
    public GameRoom createRoom() {

        String roomId = createRoomId();
        GameRoom room = new GameRoom(roomId);

        roomMap.put(roomId, room);

        return room;
    }

    /**
     * 指定された部屋を取得する。
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

        return room;
    }

    /**
     * 指定された部屋へプレイヤーを参加させる。
     *
     * @param roomId 部屋ID
     * @param player 参加するプレイヤー
     * @return 参加後の対戦部屋
     */
    public GameRoom joinRoom(
            String roomId,
            Player player) {

        GameRoom room = getRoom(roomId);

        room.addPlayer(player);

        return room;
    }

    /**
     * 指定されたプレイヤーを部屋から退出させる。
     *
     * 退出後に参加者が0人になった場合は、
     * 部屋自体も削除する。
     *
     * @param roomId 部屋ID
     * @param playerId 退出するプレイヤーID
     */
    public void leaveRoom(
            String roomId,
            String playerId) {

        GameRoom room = getRoom(roomId);

        boolean removed = room.removePlayer(playerId);

        if (!removed) {
            throw new IllegalArgumentException(
                    "指定されたプレイヤーは部屋に参加していません。"
                    + "playerId=" + playerId);
        }

        if (room.getPlayerCount() == 0) {
            removeRoom(roomId);
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
     * 外部から部屋一覧を変更されないよう、
     * 読み取り専用として返す。
     *
     * @return 対戦部屋一覧
     */
    public Collection<GameRoom> getRooms() {
        return Collections.unmodifiableCollection(
                roomMap.values());
    }

    /**
     * 新しい部屋IDを生成する。
     *
     * UUIDのハイフンを除去し、
     * 先頭8文字を部屋IDとして使用する。
     *
     * @return 部屋ID
     */
    private String createRoomId() {

        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }
    /**
     * 指定された部屋のゲームを開始する。
     *
     * 部屋に参加しているプレイヤーからゲーム状態を生成し、
     * カード配布、開始プレイヤー決定、ゲーム開始処理を実行する。
     *
     * @param roomId ゲームを開始する部屋ID
     * @return 初期化済みのゲームエンジン
     * @throws IllegalStateException 人数不足または開始済みの場合
     */
    public GameRoom startGame(String roomId) {

        GameRoom room = getRoom(roomId);

        if (room.isStarted()) {
            throw new IllegalStateException(
                    "ゲームは既に開始されています");
        }

        if (room.getPlayerCount() != REQUIRED_PLAYER_COUNT) {
            throw new IllegalStateException(
                    "ゲーム開始には"
                    + REQUIRED_PLAYER_COUNT
                    + "人必要です。現在の参加人数="
                    + room.getPlayerCount());
        }

        /*
         * 部屋の参加プレイヤーからゲーム状態を生成する。
         *
         * Playerは同じインスタンスを使用するため、
         * GameInitializerによって配布された手札は
         * GameRoom側から参照した場合にも反映される。
         */
        List<Player> players =
                new ArrayList<>(room.getPlayers());

        GameState gameState =
                new GameState(players);

        /*
         * デッキ生成、シャッフル、カード配布、
         * 手札並び替え、開始プレイヤー決定を行う。
         */
        gameInitializer.initialize(gameState);

        /*
         * 初期化済みのGameStateを使用して
         * ゲーム進行用のGameEngineを生成する。
         *
         * コンストラクタ引数は現在のGameEngineに合わせる。
         */
        GameEngine gameEngine =
        		gameEngineFactory.create();

        room.start(gameState,gameEngine);

        return room;
    }
}