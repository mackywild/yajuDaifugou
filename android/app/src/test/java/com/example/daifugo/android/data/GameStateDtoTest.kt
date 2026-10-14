package com.example.daifugo.android.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateDtoTest {

    @Test
    fun localCpuModeIncludesChallenge() {
        assertTrue(state("CPU_LOCAL").isLocalCpuGame)
        assertTrue(state("YAJU_CHALLENGE").isLocalCpuGame)
        assertFalse(state("MULTIPLAYER").isLocalCpuGame)
    }

    private fun state(mode: String) = GameStateDto(
        roomId = "test",
        gameMode = mode,
        players = emptyList(),
        currentPlayerId = null,
        field = FieldDto(),
        revolution = false,
        lockedMark = null,
        host = true,
        started = true,
        finished = false,
    )
}
