package com.example.daifugo.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

/** Web版のクラウド進行データ。初回移行以外はサーバーが報酬を確定する。 */
@Service
public class FirebaseWebProgressService {
    private static final ZoneId GAME_ZONE = ZoneId.of("Asia/Tokyo");
    private static final List<String> STAT_KEYS = List.of(
            "totalMatches", "wins", "totalRank", "yajuTargets", "yajuSuccesses",
            "challengeMatches", "challengeWins");
    private static final Map<String, String> REWARDS = Map.of(
            "play_3", "frame_silver_line",
            "win_1", "card_classic_red",
            "challenge_1", "avatar_business");

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    private DocumentReference user(String uid) {
        return db().collection("users").document(uid);
    }

    /** Firestoreに保存されているデータを取得し、存在しない場合は移行を要求する。 */
    public Map<String, Object> load(String uid) {
        DocumentSnapshot document = await(user(uid).get());
        return document.exists()
                ? Map.of("exists", true, "progress", document.get("progress"))
                : Map.of("exists", false);
    }

    /** 旧ローカルデータを一度だけ移行する。移行値は未検証データとして記録する。 */
    public Map<String, Object> migrate(String uid, Map<String, Object> legacy) {
        Map<String, Object> sanitized = sanitizeLegacy(legacy);
        return inTransaction(transaction -> {
            DocumentReference reference = user(uid);
            DocumentSnapshot snapshot = transaction.get(reference).get();
            if (!snapshot.exists()) {
                Map<String, Object> document = new HashMap<>();
                document.put("progress", sanitized);
                document.put("schemaVersion", 1);
                document.put("migrationSource", "LEGACY_UNVERIFIED");
                document.put("createdAt", FieldValue.serverTimestamp());
                transaction.set(reference, document);
                return sanitized;
            }
            return progress(snapshot);
        });
    }

    public Map<String, Object> saveProfile(String uid, String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 20) {
            throw new IllegalArgumentException("名前は1〜20文字で入力してください");
        }
        return mutate(uid, progress -> {
            progress.put("displayName", displayName.trim());
            progress.put("profileConfigured", true);
        });
    }

    public Map<String, Object> saveBgm(String uid, boolean enabled) {
        return mutate(uid, progress -> {
            Map<String, Object> settings = map(progress.get("settings"));
            settings.put("bgm", enabled);
            progress.put("settings", settings);
        });
    }

    public Map<String, Object> equip(String uid, String itemId) {
        Map<String, String> equipmentFields = Map.of(
                "avatar_business", "equippedAvatarId",
                "frame_silver_line", "equippedFrameId",
                "card_classic_red", "equippedCardSkinId",
                "effect_soft_glow", "equippedEffectId");
        String field = equipmentFields.get(itemId);
        if (field == null) {
            throw new IllegalArgumentException("不正なアイテムです");
        }
        return mutate(uid, progress -> {
            if (number(map(progress.get("inventory")).get(itemId)) < 1) {
                throw new IllegalArgumentException("未所持のアイテムは装備できません");
            }
            progress.put(field, itemId);
        });
    }

    /** Firestoreトランザクションにより報酬の二重受取を防止する。 */
    public Map<String, Object> claimMission(String uid, String missionId) {
        if (!REWARDS.containsKey(missionId)) {
            throw new IllegalArgumentException("不明なミッションです");
        }
        return mutate(uid, progress -> {
            Map<String, Object> daily = currentDaily(progress);
            int count = switch (missionId) {
                case "play_3" -> number(daily.get("matchesPlayed"));
                case "win_1" -> number(daily.get("wins"));
                case "challenge_1" -> number(daily.get("challengePlayed"));
                default -> 0;
            };
            if (count < ("play_3".equals(missionId) ? 3 : 1)) {
                throw new IllegalArgumentException("ミッションが未達成です");
            }
            List<String> claimed = strings(daily.get("claimedMissionIds"));
            if (!claimed.contains(missionId)) {
                String presentId = "mission:" + daily.get("date") + ":" + missionId;
                List<Map<String, Object>> presents = presents(progress.get("presents"));
                if (presents.stream().noneMatch(p -> presentId.equals(p.get("id")))) {
                    presents.add(Map.of("id", presentId, "title", "ミッション達成報酬",
                            "itemId", REWARDS.get(missionId), "itemName", REWARDS.get(missionId),
                            "quantity", 1, "source", missionId));
                }
                claimed.add(missionId);
                daily.put("claimedMissionIds", claimed);
                progress.put("presents", presents);
            }
            progress.put("daily", daily);
        });
    }

    public Map<String, Object> receive(String uid, String presentId) {
        return receivePresent(uid, presentId);
    }

    public Map<String, Object> receiveAll(String uid) {
        return receivePresent(uid, null);
    }

    private Map<String, Object> receivePresent(String uid, String targetId) {
        return mutate(uid, progress -> {
            List<Map<String, Object>> presents = presents(progress.get("presents"));
            Map<String, Object> inventory = map(progress.get("inventory"));
            List<Map<String, Object>> remaining = new ArrayList<>();
            for (Map<String, Object> present : presents) {
                if (targetId == null || targetId.equals(present.get("id"))) {
                    String itemId = String.valueOf(present.get("itemId"));
                    if (!List.of("avatar_business", "frame_silver_line", "card_classic_red", "effect_soft_glow").contains(itemId)) {
                        throw new IllegalArgumentException("不正な報酬アイテムです");
                    }
                    inventory.put(itemId, number(inventory.get(itemId)) + number(present.get("quantity")));
                } else {
                    remaining.add(present);
                }
            }
            progress.put("inventory", inventory);
            progress.put("presents", remaining);
        });
    }

    /**
     * サーバー確定済み順位だけを使用して経験値を付与する。
     * matchResults/{roomId} の存在確認と進行データ更新を同じトランザクションで行う。
     */
    public Map<String, Object> recordMatch(
            String uid, String roomId, int rank, boolean yajuTarget,
            boolean yajuSuccess, boolean challenge) {
        if (rank < 1 || rank > 8) {
            throw new IllegalArgumentException("順位が不正です");
        }
        DocumentReference reference = user(uid);
        DocumentReference matchReference = reference.collection("matchResults").document(roomId);
        return inTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(reference).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("プロフィールを先に作成してください");
            }
            DocumentSnapshot recorded = transaction.get(matchReference).get();
            Map<String, Object> progress = progress(snapshot);
            if (recorded.exists()) {
                return progress;
            }
            int earned = 20 + (rank == 1 ? 50 : rank == 2 ? 30 : rank == 3 ? 15 : 0)
                    + (yajuTarget ? 10 : 0) + (yajuSuccess ? 81 : 0);
            progress.put("totalExp", number(progress.get("totalExp")) + earned);
            Map<String, Object> stats = map(progress.get("stats"));
            increment(stats, "totalMatches", 1);
            increment(stats, "wins", rank == 1 ? 1 : 0);
            increment(stats, "totalRank", rank);
            increment(stats, "yajuTargets", yajuTarget ? 1 : 0);
            increment(stats, "yajuSuccesses", yajuSuccess ? 1 : 0);
            increment(stats, "challengeMatches", challenge ? 1 : 0);
            increment(stats, "challengeWins", challenge && rank == 1 ? 1 : 0);
            progress.put("stats", stats);
            Map<String, Object> daily = currentDaily(progress);
            increment(daily, "matchesPlayed", 1);
            increment(daily, "wins", rank == 1 ? 1 : 0);
            increment(daily, "challengePlayed", challenge ? 1 : 0);
            progress.put("daily", daily);
            transaction.update(reference, "progress", progress);
            transaction.set(matchReference, Map.of("rank", rank, "earnedExp", earned,
                    "yajuTarget", yajuTarget, "yajuSuccess", yajuSuccess,
                    "challenge", challenge, "finishedAt", FieldValue.serverTimestamp()));
            return progress;
        });
    }

    private interface ProgressChange {
        void apply(Map<String, Object> progress);
    }

    private Map<String, Object> mutate(String uid, ProgressChange change) {
        return inTransaction(transaction -> {
            DocumentReference reference = user(uid);
            DocumentSnapshot snapshot = transaction.get(reference).get();
            if (!snapshot.exists()) {
                throw new IllegalStateException("プロフィールを先に作成してください");
            }
            Map<String, Object> progress = progress(snapshot);
            change.apply(progress);
            transaction.update(reference, "progress", progress);
            return progress;
        });
    }

    private <T> T inTransaction(Transaction.Function<T> action) {
        return await(db().runTransaction(action));
    }

    private static <T> T await(com.google.api.core.ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore通信が中断されました", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Firestore保存に失敗しました", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> data
                ? new HashMap<>((Map<String, Object>) data)
                : new HashMap<>();
    }

    private static Map<String, Object> progress(DocumentSnapshot snapshot) {
        return map(snapshot.get("progress"));
    }

    private static void increment(Map<String, Object> values, String key, int delta) {
        values.put(key, number(values.get(key)) + delta);
    }

    private static int number(Object value) {
        return value instanceof Number numeric ? numeric.intValue() : 0;
    }

    private static int bounded(Object value, int max) {
        return Math.max(0, Math.min(max, number(value)));
    }

    private static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof String text && text.length() <= 100 && result.size() < 200) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private static List<Map<String, Object>> presents(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                Map<String, Object> present = map(item);
                if (!present.isEmpty()) {
                    result.add(present);
                }
            }
        }
        return result;
    }

    private static Map<String, Object> currentDaily(Map<String, Object> progress) {
        String today = LocalDate.now(GAME_ZONE).toString();
        Map<String, Object> daily = map(progress.get("daily"));
        if (!today.equals(daily.get("date"))) {
            daily = new HashMap<>(Map.of("date", today,
                    "matchesPlayed", 0, "wins", 0, "challengePlayed", 0,
                    "claimedMissionIds", new ArrayList<>()));
        }
        return daily;
    }

    /** 旧データは未検証として保持し、フィールド・サイズ・数値を制限する。 */
    static Map<String, Object> sanitizeLegacy(Map<String, Object> input) {
        Map<String, Object> legacy = input == null ? Map.of() : input;
        Map<String, Object> result = new HashMap<>();
        String displayName = String.valueOf(legacy.getOrDefault("displayName", "PLAYER")).trim();
        if (displayName.isBlank()) displayName = "PLAYER";
        result.put("displayName", displayName.substring(0, Math.min(20, displayName.length())));
        result.put("profileConfigured", Boolean.TRUE.equals(legacy.get("profileConfigured")));
        result.put("totalExp", bounded(legacy.get("totalExp"), 10000000));
        Map<String, Object> stats = new HashMap<>();
        Map<String, Object> oldStats = map(legacy.get("stats"));
        for (String key : STAT_KEYS) {
            stats.put(key, bounded(oldStats.get(key), 10000000));
        }
        result.put("stats", stats);
        Map<String, Object> inventory = new HashMap<>();
        Map<String, Object> oldInventory = map(legacy.get("inventory"));
        for (String key : List.of("avatar_business", "frame_silver_line", "card_classic_red", "effect_soft_glow")) {
            inventory.put(key, bounded(oldInventory.get(key), 9999));
        }
        result.put("inventory", inventory);
        Map<String, Object> oldSettings = map(legacy.get("settings"));
        result.put("settings", Map.of("bgm", !Boolean.FALSE.equals(oldSettings.get("bgm"))));
        for (String field : List.of("equippedAvatarId", "equippedFrameId",
                "equippedCardSkinId", "equippedEffectId")) {
            Object itemId = legacy.get(field);
            result.put(field, itemId instanceof String id && number(inventory.get(id)) > 0 ? id : "");
        }
        Map<String, Object> oldDaily = map(legacy.get("daily"));
        Map<String, Object> daily = currentDaily(Map.of("daily", oldDaily));
        if (daily.get("date").equals(oldDaily.get("date"))) {
            for (String key : List.of("matchesPlayed", "wins", "challengePlayed")) {
                daily.put(key, bounded(oldDaily.get(key), 100000));
            }
            daily.put("claimedMissionIds", strings(oldDaily.get("claimedMissionIds"))
                    .stream().filter(REWARDS::containsKey).distinct().toList());
        }
        result.put("daily", daily);
        List<Map<String, Object>> migratedPresents = new ArrayList<>();
        for (Map<String, Object> present : presents(legacy.get("presents"))) {
            String itemId = String.valueOf(present.getOrDefault("itemId", ""));
            String id = String.valueOf(present.getOrDefault("id", ""));
            if (REWARDS.containsValue(itemId) && id.length() <= 100
                    && !id.isBlank() && migratedPresents.size() < 100) {
                migratedPresents.add(Map.of("id", id, "title", "引継ぎ報酬",
                        "itemId", itemId, "itemName", itemId,
                        "quantity", Math.max(1, Math.min(99, number(present.get("quantity")))),
                        "source", "旧データ"));
            }
        }
        result.put("presents", migratedPresents);
        result.put("seenMatches", strings(legacy.get("seenMatches")));
        return result;
    }
}
