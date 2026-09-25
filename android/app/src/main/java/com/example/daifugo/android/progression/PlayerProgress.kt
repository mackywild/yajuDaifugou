package com.example.daifugo.android.progression

import kotlin.math.max

data class PlayerProgress(
    val accountKey: String = "guest",
    val displayName: String = "GUEST",
    val totalExp: Int = 0,
    val stats: MatchStats = MatchStats(),
) {
    val level: Int get() = ProgressionRules.levelFor(totalExp)
    val expIntoLevel: Int get() = ProgressionRules.expIntoLevel(totalExp)
    val expToNextLevel: Int get() = ProgressionRules.expRequiredForLevel(level)

    val winRatePercent: Double
        get() = if (stats.totalMatches == 0) 0.0 else stats.wins * 100.0 / stats.totalMatches

    val averageRank: Double
        get() = if (stats.totalMatches == 0) 0.0 else stats.totalRank.toDouble() / stats.totalMatches

    val yajuSuccessRatePercent: Double
        get() = if (stats.yajuTargets == 0) 0.0 else stats.yajuSuccesses * 100.0 / stats.yajuTargets
}

data class MatchStats(
    val totalMatches: Int = 0,
    val wins: Int = 0,
    val totalRank: Int = 0,
    val yajuTargets: Int = 0,
    val yajuSuccesses: Int = 0,
    val challengeMatches: Int = 0,
    val challengeWins: Int = 0,
)

data class MatchProgressResult(
    val progress: PlayerProgress,
    val earnedExp: Int,
)

object ProgressionRules {
    const val MATCH_COMPLETE_EXP = 20
    const val FIRST_PLACE_EXP = 50
    const val SECOND_PLACE_EXP = 30
    const val THIRD_PLACE_EXP = 15
    const val YAJU_TARGET_EXP = 10
    const val YAJU_SUCCESS_EXP = 81

    fun earnedExp(rank: Int, yajuTarget: Boolean, yajuSuccess: Boolean): Int {
        val placement = when (rank) {
            1 -> FIRST_PLACE_EXP
            2 -> SECOND_PLACE_EXP
            3 -> THIRD_PLACE_EXP
            else -> 0
        }
        return MATCH_COMPLETE_EXP +
            placement +
            (if (yajuTarget) YAJU_TARGET_EXP else 0) +
            (if (yajuSuccess) YAJU_SUCCESS_EXP else 0)
    }

    fun expRequiredForLevel(level: Int): Int = 100 + max(0, level - 1) * 50

    fun levelFor(totalExp: Int): Int {
        var remaining = max(0, totalExp)
        var level = 1
        while (remaining >= expRequiredForLevel(level)) {
            remaining -= expRequiredForLevel(level)
            level++
        }
        return level
    }

    fun expIntoLevel(totalExp: Int): Int {
        var remaining = max(0, totalExp)
        var level = 1
        while (remaining >= expRequiredForLevel(level)) {
            remaining -= expRequiredForLevel(level)
            level++
        }
        return remaining
    }
}
