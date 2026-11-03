package com.example.daifugo.web;

import java.util.Map;

import com.example.daifugo.game.domain.Player;
import com.example.daifugo.game.domain.YajuStatus;
import com.example.daifugo.game.mode.GameMode;
import com.example.daifugo.room.GameRoom;
import com.example.daifugo.room.GameRoomService;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Firebase UIDに紐付くWebブラウザ版の進行データAPI。 */
@RestController
@RequestMapping("/api/users/me")
public class FirebaseWebProgressController {
    private final FirebaseWebProgressService progressService;
    private final GameRoomService roomService;

    public FirebaseWebProgressController(
            FirebaseWebProgressService progressService,
            GameRoomService roomService) {
        this.progressService = progressService;
        this.roomService = roomService;
    }

    @GetMapping("/progress")
    public Map<String, Object> load(HttpSession session) {
        return progressService.load(requireUid(session));
    }

    @PostMapping("/progress/migrate")
    public Map<String, Object> migrate(
            @RequestBody Map<String, Object> legacy, HttpSession session) {
        return Map.of("progress", progressService.migrate(requireUid(session), legacy));
    }

    @PatchMapping("/profile")
    public Map<String, Object> profile(
            @RequestBody ProfileRequest request, HttpSession session) {
        return Map.of("progress", progressService.saveProfile(requireUid(session), request.displayName()));
    }

    @PatchMapping("/settings")
    public Map<String, Object> settings(
            @RequestBody SettingsRequest request, HttpSession session) {
        return Map.of("progress", progressService.saveBgm(requireUid(session), request.bgm()));
    }

    @PostMapping("/equipment")
    public Map<String, Object> equipment(
            @RequestBody EquipmentRequest request, HttpSession session) {
        return Map.of("progress", progressService.equip(requireUid(session), request.itemId()));
    }

    @PostMapping("/missions/{missionId}/claim")
    public Map<String, Object> claim(
            @PathVariable String missionId, HttpSession session) {
        return Map.of("progress", progressService.claimMission(requireUid(session), missionId));
    }

    @PostMapping("/presents/{presentId}/receive")
    public Map<String, Object> receive(
            @PathVariable String presentId, HttpSession session) {
        return Map.of("progress", progressService.receive(requireUid(session), presentId));
    }

    @PostMapping("/presents/receive-all")
    public Map<String, Object> receiveAll(HttpSession session) {
        return Map.of("progress", progressService.receiveAll(requireUid(session)));
    }

    /** クライアントの自己申告ではなく、現在のゲームサーバーが確定した順位を保存する。 */
    @PostMapping("/matches/{roomId}/record")
    public Map<String, Object> record(
            @PathVariable String roomId, HttpSession session) {
        String uid = requireUid(session);
        Object activeRoomId = session.getAttribute(GameApiController.SESSION_ROOM_ID);
        Object activePlayerId = session.getAttribute(GameApiController.SESSION_PLAYER_ID);
        if (!roomId.equals(activeRoomId) || !(activePlayerId instanceof String playerId)) {
            throw new IllegalArgumentException("この対戦のプレイヤーではありません");
        }
        GameRoom room = roomService.getRoom(roomId);
        int rank;
        boolean yajuTarget;
        boolean yajuSuccess;
        boolean challenge;
        synchronized (room) {
            if (!room.isFinished()) {
                throw new IllegalStateException("まだ対戦が終了していません");
            }
            Player player = room.getGameState().getPlayers().stream()
                    .filter(candidate -> playerId.equals(candidate.getId()) && !candidate.isCpu())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("参加プレイヤーが見つかりません"));
            if (player.getRank() == null) {
                throw new IllegalStateException("順位が確定していません");
            }
            rank = player.getRank();
            yajuTarget = player.isYajuTarget();
            yajuSuccess = player.getYajuStatus() == YajuStatus.COMPLETED;
            challenge = room.getGameMode() == GameMode.YAJU_CHALLENGE
                    || room.getGameMode() == GameMode.MULTI_YAJU_CHALLENGE;
        }
        return Map.of("progress", progressService.recordMatch(
                uid, roomId, rank, yajuTarget, yajuSuccess, challenge));
    }

    private static String requireUid(HttpSession session) {
        Object uid = session.getAttribute(AuthController.FIREBASE_UID_SESSION_KEY);
        if (!(uid instanceof String firebaseUid) || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Googleログインが必要です");
        }
        return firebaseUid;
    }

    public record ProfileRequest(String displayName) {}
    public record SettingsRequest(boolean bgm) {}
    public record EquipmentRequest(String itemId) {}
}
