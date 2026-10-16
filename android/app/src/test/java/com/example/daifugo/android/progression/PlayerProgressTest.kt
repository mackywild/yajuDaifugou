package com.example.daifugo.android.progression

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProgressTest {
    @Test
    fun profileMustBeConfiguredExplicitly() {
        assertFalse(PlayerProgress().profileConfigured)
        assertTrue(
            PlayerProgress(
                accountKey = "pgs:test",
                displayName = "PLAYER",
                profileConfigured = true,
            ).profileConfigured
        )
    }
}
