package com.example.daifugo.android.firebase

import com.example.daifugo.android.cosmetics.CosmeticCatalog
import com.example.daifugo.android.progression.DailyMissionState
import com.example.daifugo.android.progression.MatchStats
import com.example.daifugo.android.progression.PlayerProgress
import com.example.daifugo.android.progression.PresentEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CloudProgressSnapshot(
    val progress: PlayerProgress,
    val bgmEnabled: Boolean,
)

/**
 * Web版のFirestoreスキーマとAndroidの進行データを相互変換する。
 * ローカルデータの移行はサーバー側で未検証データ扱いとなる。
 */
object CloudProgressCodec {
    fun decode(cloud: JSONObject, uid: String): CloudProgressSnapshot {
        val stats = cloud.optJSONObject("stats") ?: JSONObject()
        val daily = cloud.optJSONObject("daily") ?: JSONObject()
        val inventoryJson = cloud.optJSONObject("inventory") ?: JSONObject()
        val inventory = buildMap {
            val keys = inventoryJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, inventoryJson.optInt(key).coerceAtLeast(0))
            }
        }
        val presentsJson = cloud.optJSONArray("presents") ?: JSONArray()
        val presents = (0 until presentsJson.length()).mapNotNull { index ->
            val present = presentsJson.optJSONObject(index) ?: return@mapNotNull null
            val itemId = present.optString("itemId")
            val presentId = present.optString("id")
            if (itemId.isBlank() || presentId.isBlank()) return@mapNotNull null
            PresentEntry(
                id = presentId,
                title = present.optString("title", "プレゼント"),
                itemId = itemId,
                itemName = CosmeticCatalog.find(itemId)?.displayName
                    ?: present.optString("itemName", itemId),
                quantity = present.optInt("quantity", 1).coerceAtLeast(1),
                source = present.optString("source"),
            )
        }
        val claimed = daily.optJSONArray("claimedMissionIds") ?: JSONArray()
        val today = LocalDate.now().toString()
        val dailyState = if (daily.optString("date") == today) {
            DailyMissionState(
                date = today,
                matchesPlayed = daily.optInt("matchesPlayed"),
                wins = daily.optInt("wins"),
                challengePlayed = daily.optInt("challengePlayed"),
                claimedMissionIds = (0 until claimed.length())
                    .map { claimed.optString(it) }.filter { it.isNotBlank() }.toSet(),
            )
        } else {
            DailyMissionState.today()
        }

        fun optionalString(key: String): String? =
            cloud.optString(key).takeIf { it.isNotBlank() && it != "null" }

        return CloudProgressSnapshot(
            progress = PlayerProgress(
                accountKey = "firebase:$uid",
                displayName = cloud.optString("displayName", "PLAYER"),
                profileConfigured = cloud.optBoolean("profileConfigured"),
                totalExp = cloud.optInt("totalExp").coerceAtLeast(0),
                inventory = inventory,
                presents = presents,
                equippedAvatarId = optionalString("equippedAvatarId"),
                equippedFrameId = optionalString("equippedFrameId"),
                equippedCardSkinId = optionalString("equippedCardSkinId"),
                equippedEffectId = optionalString("equippedEffectId"),
                stats = MatchStats(
                    totalMatches = stats.optInt("totalMatches"),
                    wins = stats.optInt("wins"),
                    totalRank = stats.optInt("totalRank"),
                    yajuTargets = stats.optInt("yajuTargets"),
                    yajuSuccesses = stats.optInt("yajuSuccesses"),
                    challengeMatches = stats.optInt("challengeMatches"),
                    challengeWins = stats.optInt("challengeWins"),
                ),
                daily = dailyState,
            ),
            bgmEnabled = cloud.optJSONObject("settings")?.optBoolean("bgm", true) ?: true,
        )
    }

    /** 引継ぎ元の端末データをWeb版と同じ形式で送信する。 */
    fun encodeMigration(progress: PlayerProgress, bgmEnabled: Boolean): JSONObject = JSONObject()
        .put("displayName", progress.displayName)
        .put("profileConfigured", progress.profileConfigured)
        .put("totalExp", progress.totalExp)
        .put("inventory", JSONObject(progress.inventory))
        .put("equippedAvatarId", progress.equippedAvatarId ?: "")
        .put("equippedFrameId", progress.equippedFrameId ?: "")
        .put("equippedCardSkinId", progress.equippedCardSkinId ?: "")
        .put("equippedEffectId", progress.equippedEffectId ?: "")
        .put("settings", JSONObject().put("bgm", bgmEnabled))
        .put("stats", JSONObject().apply {
            put("totalMatches", progress.stats.totalMatches)
            put("wins", progress.stats.wins)
            put("totalRank", progress.stats.totalRank)
            put("yajuTargets", progress.stats.yajuTargets)
            put("yajuSuccesses", progress.stats.yajuSuccesses)
            put("challengeMatches", progress.stats.challengeMatches)
            put("challengeWins", progress.stats.challengeWins)
        })
        .put("daily", JSONObject().apply {
            put("date", progress.daily.date)
            put("matchesPlayed", progress.daily.matchesPlayed)
            put("wins", progress.daily.wins)
            put("challengePlayed", progress.daily.challengePlayed)
            put("claimedMissionIds", JSONArray(progress.daily.claimedMissionIds.toList()))
        })
        .put("presents", JSONArray().apply {
            progress.presents.forEach { present ->
                put(JSONObject().apply {
                    put("id", present.id)
                    put("title", present.title)
                    put("itemId", present.itemId)
                    put("itemName", present.itemName)
                    put("quantity", present.quantity)
                    put("source", present.source)
                })
            }
        })
}
