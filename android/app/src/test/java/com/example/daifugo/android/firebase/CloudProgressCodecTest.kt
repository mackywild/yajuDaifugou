package com.example.daifugo.android.firebase

import com.example.daifugo.android.progression.DailyMissionState
import com.example.daifugo.android.progression.MatchStats
import com.example.daifugo.android.progression.PlayerProgress
import com.example.daifugo.android.progression.PresentEntry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Web版Firestore進行データとAndroidローカル進行データの整合性を確認する。 */
class CloudProgressCodecTest {
    @Test
    fun webProgressRetainsStatsInventoryAndDaily() {
        val today = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString()
        val cloud = JSONObject()
            .put("displayName", "WEB_PLAYER")
            .put("profileConfigured", true)
            .put("totalExp", 999)
            .put("inventory", JSONObject().put("frame_silver_line", 2))
            .put("equippedFrameId", "frame_silver_line")
            .put("stats", JSONObject().put("totalMatches", 6).put("wins", 3))
            .put("daily", JSONObject()
                .put("date", today)
                .put("matchesPlayed", 2)
                .put("claimedMissionIds", JSONArray().put("play_3")))
            .put("settings", JSONObject().put("bgm", false))
        val decoded = CloudProgressCodec.decode(cloud, "firebase-user-a")
        assertEquals("firebase:firebase-user-a", decoded.progress.accountKey)
        assertEquals("WEB_PLAYER", decoded.progress.displayName)
        assertEquals(999, decoded.progress.totalExp)
        assertEquals(6, decoded.progress.stats.totalMatches)
        assertEquals(2, decoded.progress.inventory["frame_silver_line"])
        assertEquals("frame_silver_line", decoded.progress.equippedFrameId)
        assertEquals(2, decoded.progress.daily.matchesPlayed)
        assertTrue("play_3" in decoded.progress.daily.claimedMissionIds)
        assertFalse(decoded.bgmEnabled)
    }

    @Test
    fun differentFirebaseUidNeverReusesPreviousIdentity() {
        val cloud = JSONObject().put("displayName", "PLAYER_A")
        val accountA = CloudProgressCodec.decode(cloud, "a")
        val accountB = CloudProgressCodec.decode(cloud, "b")
        assertEquals("firebase:a", accountA.progress.accountKey)
        assertEquals("firebase:b", accountB.progress.accountKey)
        assertNull(accountB.progress.equippedAvatarId)
    }

    @Test
    fun migrationSerializesLocalValuesInWebSchema() {
        val legacy = PlayerProgress(
            displayName = "ANDROID_PLAYER",
            profileConfigured = true,
            totalExp = 123,
            inventory = mapOf("frame_silver_line" to 1),
            stats = MatchStats(totalMatches = 7, wins = 3),
            daily = DailyMissionState(
                date = LocalDate.now(ZoneId.of("Asia/Tokyo")).toString(),
                matchesPlayed = 1,
                claimedMissionIds = setOf("win_1"),
            ),
            presents = listOf(PresentEntry(
                id = "reward-1",
                title = "報酬",
                itemId = "frame_silver_line",
                itemName = "シルバーライン",
                source = "test",
            )),
        )
        val cloud = CloudProgressCodec.encodeMigration(legacy, false)
        assertEquals("ANDROID_PLAYER", cloud.getString("displayName"))
        assertEquals(123, cloud.getInt("totalExp"))
        assertEquals(7, cloud.getJSONObject("stats").getInt("totalMatches"))
        assertEquals(1, cloud.getJSONObject("inventory").getInt("frame_silver_line"))
        assertEquals("reward-1", cloud.getJSONArray("presents").getJSONObject(0).getString("id"))
        assertEquals("win_1", cloud.getJSONObject("daily").getJSONArray("claimedMissionIds").getString(0))
        assertFalse(cloud.getJSONObject("settings").getBoolean("bgm"))
    }
}
