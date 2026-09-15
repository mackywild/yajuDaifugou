package com.example.daifugo.web;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.daifugo.game.config.GameRuleSettings;
import com.example.daifugo.game.cpu.CpuDifficulty;
import com.example.daifugo.game.cpu.CpuGameService;
import com.example.daifugo.game.domain.Card;
import com.example.daifugo.game.mode.GameMode;
import com.example.daifugo.game.domain.Mark;
import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.Rank;
import com.example.daifugo.response.GameStateResponse;
import com.example.daifugo.response.GameStateResponseMapper;
import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

/**
 * ブラウザ版大富豪で使用するゲームAPI。
 *
 * プレイヤーIDをクライアント入力として信用せず、
 * HTTPセッションに紐付けた本人情報を使用してゲーム操作を行う。
 */
@RestController
@RequestMapping("/api/rooms")
public class GameApiController {

    /** セッションに保存する部屋ID */
    static final String SESSION_ROOM_ID = "DAIFUGO_ROOM_ID";

    /** セッションに保存するプレイヤーID */
    static final String SESSION_PLAYER_ID = "DAIFUGO_PLAYER_ID";

    private final GameRoomService roomService;
    private final CpuGameService cpuGameService;
    private final GameWebSocketHandler webSocketHandler;
    private final GameStateResponseMapper responseMapper = new GameStateResponseMapper();

    public GameApiController(
            GameRoomService roomService,
            CpuGameService cpuGameService,
            GameWebSocketHandler webSocketHandler) {
        this.roomService = roomService;
        this.cpuGameService = cpuGameService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 新しい部屋を作成し、作成者をホストとして参加させる。
     */
    @PostMapping
    public JoinResponse createRoom(
            @RequestBody PlayerNameRequest request,
            HttpSession session) {

        ensureNotBoundToRoom(session);
        validatePlayerName(request.playerName());

        GameRoom room = roomService.createRoom();
        Player player = createPlayer(request.playerName());
        roomService.joinRoom(room.getRoomId(), player);
        bindSession(session, room.getRoomId(), player.getId());
        webSocketHandler.broadcastStateChanged(room.getRoomId());

        return new JoinResponse(room.getRoomId(), true);
    }

    /**
     * CPU戦【ひとりでイク】を作成し、その場でゲームを開始する。
     */
    @PostMapping("/cpu")
    public GameStateResponse createCpuGame(
            @RequestBody CpuGameRequest request,
            HttpSession session) {

        ensureNotBoundToRoom(session);
        validatePlayerName(request.playerName());

        CpuDifficulty difficulty;
        try {
            difficulty = CpuDifficulty.valueOf(request.difficulty());
        } catch (Exception ex) {
            throw new IllegalArgumentException("CPU難易度が不正です");
        }

        GameRuleSettings settings = new GameRuleSettings(
                request.jokerCount(),
                request.revolution(),
                request.eightCut(),
                request.markLock(),
                request.sevenTransfer(),
                request.yajuRule(),
                request.jackBack(),
                request.forbiddenFinish()
        );

        CpuGameService.CpuGameCreation creation = cpuGameService.createGame(
                request.playerName().trim(),
                request.cpuCount(),
                difficulty,
                settings
        );
        GameRoom room = creation.room();
        Player human = creation.humanPlayer();
        bindSession(session, room.getRoomId(), human.getId());

        synchronized (room) {
            return responseMapper.map(room, human.getId());
        }
    }

    /**
     * 既存部屋へ参加し、セッションとプレイヤーを紐付ける。
     */
    @PostMapping("/{roomId}/join")
    public JoinResponse joinRoom(
            @PathVariable String roomId,
            @RequestBody PlayerNameRequest request,
            HttpSession session) {

        ensureNotBoundToRoom(session);
        validatePlayerName(request.playerName());

        Player player = createPlayer(request.playerName());
        GameRoom room = roomService.joinRoom(roomId, player);
        bindSession(session, roomId, player.getId());
        webSocketHandler.broadcastStateChanged(roomId);

        return new JoinResponse(roomId, room.isHost(player.getId()));
    }

    /**
     * 部屋の現在状態を取得する。
     * 誰の視点で見るかはHTTPセッションから決定する。
     */
    @GetMapping("/{roomId}/state")
    public GameStateResponse state(
            @PathVariable String roomId,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        GameRoom room = roomService.getRoom(roomId);

        synchronized (room) {
            ensureMember(room, playerId);
            return responseMapper.map(room, playerId);
        }
    }

    /**
     * ホストがゲームを開始する。
     */
    @PostMapping("/{roomId}/start")
    public GameStateResponse start(
            @PathVariable String roomId,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        GameRoom room = roomService.getRoom(roomId);

        GameStateResponse response;
        synchronized (room) {
            ensureMember(room, playerId);
            if (!room.isHost(playerId)) {
                throw new IllegalStateException("ゲームを開始できるのはホストだけです");
            }

            roomService.startGame(roomId);
            response = responseMapper.map(room, playerId);
        }
        webSocketHandler.broadcastStateChanged(roomId);
        return response;
    }

    /**
     * 選択したカードを場へ出す。
     */
    @PostMapping("/{roomId}/play")
    public GameStateResponse play(
            @PathVariable String roomId,
            @RequestBody PlayRequest request,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        GameRoom room = roomService.getRoom(roomId);
        ensureStarted(room);

        if (request.cards() == null || request.cards().isEmpty()) {
            throw new IllegalArgumentException("カードを選択してください");
        }

        List<Card> selectedCards = request.cards().stream()
                .map(this::toCard)
                .toList();

        roomService.play(roomId, playerId, selectedCards);
        processCpuIfNeeded(room);

        GameStateResponse response;
        synchronized (room) {
            response = responseMapper.map(room, playerId);
        }
        webSocketHandler.broadcastStateChanged(roomId);
        return response;
    }

    /**
     * 7渡しで選択したカードを隣のプレイヤーへ渡す。
     *
     * 7渡しの必要枚数と渡し先はサーバー側GameStateで管理する。
     * クライアントはカードだけを選択し、本人情報はHTTPセッションから取得する。
     */
    @PostMapping("/{roomId}/seven-transfer")
    public GameStateResponse sevenTransfer(
            @PathVariable String roomId,
            @RequestBody PlayRequest request,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        GameRoom room = roomService.getRoom(roomId);
        ensureStarted(room);

        if (request.cards() == null || request.cards().isEmpty()) {
            throw new IllegalArgumentException("7渡しするカードを選択してください");
        }

        List<Card> selectedCards = request.cards().stream()
                .map(this::toCard)
                .toList();

        roomService.transferSeven(roomId, playerId, selectedCards);
        processCpuIfNeeded(room);

        GameStateResponse response;
        synchronized (room) {
            response = responseMapper.map(room, playerId);
        }
        webSocketHandler.broadcastStateChanged(roomId);
        return response;
    }

    /**
     * 現在手番のプレイヤーがパスする。
     */
    @PostMapping("/{roomId}/pass")
    public GameStateResponse pass(
            @PathVariable String roomId,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        GameRoom room = roomService.getRoom(roomId);
        ensureStarted(room);

        roomService.pass(roomId, playerId);
        processCpuIfNeeded(room);

        GameStateResponse response;
        synchronized (room) {
            response = responseMapper.map(room, playerId);
        }
        webSocketHandler.broadcastStateChanged(roomId);
        return response;
    }

    /**
     * ゲーム開始前またはゲーム終了後に部屋から退出する。
     * 最後の参加者が退出した場合は部屋も削除される。
     */
    @PostMapping("/{roomId}/leave")
    public void leave(
            @PathVariable String roomId,
            HttpSession session) {

        String playerId = requireBoundPlayer(session, roomId);
        roomService.leaveRoom(roomId, playerId);
        clearRoomBinding(session);
        webSocketHandler.broadcastStateChanged(roomId);
    }

    /** CPU戦の場合、人間の操作後に次の人間手番までCPUを自動進行する。 */
    private void processCpuIfNeeded(GameRoom room) {
        if (room.getGameMode() == GameMode.CPU && !room.isFinished()) {
            cpuGameService.processCpuTurns(room.getRoomId());
        }
    }

    /**
     * 通信DTOからカードを生成する。
     */
    private Card toCard(CardRequest card) {
        if (card == null || card.suit() == null || card.rank() == null) {
            throw new IllegalArgumentException("カード情報が不正です");
        }

        try {
            return new Card(
                    Mark.valueOf(card.suit()),
                    Rank.valueOf(card.rank()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("カード情報が不正です", e);
        }
    }

    private Player createPlayer(String playerName) {
        return new Player(
                UUID.randomUUID().toString(),
                playerName.trim());
    }

    private void validatePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("プレイヤー名を入力してください");
        }
        if (playerName.length() > 20) {
            throw new IllegalArgumentException("プレイヤー名は20文字以内にしてください");
        }
    }

    private void ensureStarted(GameRoom room) {
        synchronized (room) {
            if (!room.isStarted() || room.getGameState() == null || room.getGameEngine() == null) {
                throw new IllegalStateException("ゲームが開始されていません");
            }
        }
    }

    private void ensureMember(GameRoom room, String playerId) {
        if (!room.containsPlayer(playerId)) {
            throw new IllegalArgumentException("この部屋に参加していないプレイヤーです");
        }
    }

    private void ensureNotBoundToRoom(HttpSession session) {
        Object boundPlayerId = session.getAttribute(SESSION_PLAYER_ID);
        Object boundRoomId = session.getAttribute(SESSION_ROOM_ID);

        if (boundPlayerId == null) {
            return;
        }

        if (boundRoomId instanceof String roomId) {
            try {
                roomService.getRoom(roomId);
                throw new IllegalStateException("既に別の部屋へ参加しています。先に退出してください");
            } catch (IllegalArgumentException expiredRoom) {
                // TTLなどで部屋が既に消えている場合は古いセッション紐付けだけ破棄する。
                clearRoomBinding(session);
                return;
            }
        }

        clearRoomBinding(session);
    }

    private void bindSession(HttpSession session, String roomId, String playerId) {
        session.setAttribute(SESSION_ROOM_ID, roomId);
        session.setAttribute(SESSION_PLAYER_ID, playerId);
    }

    private String requireBoundPlayer(HttpSession session, String roomId) {
        Object sessionRoomId = session.getAttribute(SESSION_ROOM_ID);
        Object sessionPlayerId = session.getAttribute(SESSION_PLAYER_ID);

        if (!(sessionRoomId instanceof String boundRoomId)
                || !(sessionPlayerId instanceof String playerId)
                || !boundRoomId.equals(roomId)) {
            throw new IllegalStateException("このブラウザは指定された部屋に参加していません");
        }

        return playerId;
    }

    private void clearRoomBinding(HttpSession session) {
        session.removeAttribute(SESSION_ROOM_ID);
        session.removeAttribute(SESSION_PLAYER_ID);
    }

    public record PlayerNameRequest(String playerName) {
    }

    public record CardRequest(String suit, String rank) {
    }

    public record PlayRequest(List<CardRequest> cards) {
    }

    public record CpuGameRequest(
            String playerName,
            int cpuCount,
            String difficulty,
            int jokerCount,
            boolean revolution,
            boolean eightCut,
            boolean markLock,
            boolean sevenTransfer,
            boolean yajuRule,
            boolean jackBack,
            boolean forbiddenFinish
    ) {
    }

    public record JoinResponse(String roomId, boolean host) {
    }
}
