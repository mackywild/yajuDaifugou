package com.example.daifugo.android.progression

import com.example.daifugo.android.cosmetics.CosmeticCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyMissionRulesTest {
    @Test
    fun completedDailyMissionExposesItemReward() {
        val state = DailyMissionState(
            date = "2026-10-08",
            matchesPlayed = 3,
        )

        val mission = DailyMissionRules.views(state)
            .first { it.id == DailyMissionRules.PLAY_3 }

        assertTrue(mission.completed)
        assertTrue(mission.claimable)
        assertEquals(CosmeticCatalog.FRAME_SILVER_LINE, mission.rewardItemId)
        assertEquals("シルバーライン", mission.rewardItemName)
    }
}
