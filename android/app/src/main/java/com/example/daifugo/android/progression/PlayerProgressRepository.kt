package com.example.daifugo.android.progression

import android.content.Context
import org.json.JSONObject

class PlayerProgressRepository(context: Context) {
    private val preferences =
        context.getSharedPreferences("daifugo_progression", Context.MODE_PRIVATE)

    fun load(): PlayerProgress = loadFor(activeAccountKey())

    fun bindPlayGamesAccount(playerId: String, displayName: String): PlayerProgress {
        require(playerId.isNotBlank()) { "playerId must not be blank" }
        val key = "pgs:$playerId"
        val existing = preferences.getString(profileKey(key), null)
        val progress = if (existing == null) {
            val guest = loadFor("guest")
            guest.copy(accountKey = key, displayName = displayName.ifBlank { "PLAYER" })
        } else {
            val decoded = decode(existing)
            decoded.copy(displayName = displayName.ifBlank { decoded.displayName })
        }

        saveFor(key, progress)
        preferences.edit().putString(KEY_ACTIVE_ACCOUNT, key).apply()
        return progress
    }

    fun useGuest(): PlayerProgress {
        preferences.edit().putString(KEY_ACTIVE_ACCOUNT, "guest").apply()
        return loadFor("guest")
    }

    fun recordMatch(
        matchId: String,
        rank: Int,
        playerCount: Int,
        yajuStatus: String,
        challenge: Boolean,
    ): MatchProgressResult? {
        require(rank in 1..playerCount) { "rank must be within player count" }

        val accountKey = activeAccountKey()
        val seen = preferences.getStringSet(seenMatchesKey(accountKey), emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

        if (!seen.add(matchId)) {
            return null
        }

        val current = loadFor(accountKey)
        val yajuTarget = yajuStatus != "NONE"
        val yajuSuccess = yajuStatus == "COMPLETED"
        val earned = ProgressionRules.earnedExp(rank, yajuTarget, yajuSuccess)

        val stats = current.stats.copy(
            totalMatches = current.stats.totalMatches + 1,
            wins = current.stats.wins + if (rank == 1) 1 else 0,
            totalRank = current.stats.totalRank + rank,
            yajuTargets = current.stats.yajuTargets + if (yajuTarget) 1 else 0,
            yajuSuccesses = current.stats.yajuSuccesses + if (yajuSuccess) 1 else 0,
            challengeMatches = current.stats.challengeMatches + if (challenge) 1 else 0,
            challengeWins = current.stats.challengeWins + if (challenge && rank == 1) 1 else 0,
        )
        val updated = current.copy(
            totalExp = current.totalExp + earned,
            stats = stats,
        )

        saveFor(accountKey, updated)
        preferences.edit()
            .putStringSet(seenMatchesKey(accountKey), seen.toList().takeLast(MAX_SEEN_MATCHES).toSet())
            .apply()

        return MatchProgressResult(updated, earned)
    }

    private fun activeAccountKey(): String =
        preferences.getString(KEY_ACTIVE_ACCOUNT, "guest") ?: "guest"

    private fun loadFor(accountKey: String): PlayerProgress {
        val raw = preferences.getString(profileKey(accountKey), null)
        if (raw == null) {
            val created = PlayerProgress(
                accountKey = accountKey,
                displayName = if (accountKey == "guest") "GUEST" else "PLAYER",
            )
            saveFor(accountKey, created)
            return created
        }
        return runCatching { decode(raw) }
            .getOrElse {
                PlayerProgress(
                    accountKey = accountKey,
                    displayName = if (accountKey == "guest") "GUEST" else "PLAYER",
                )
            }
    }

    private fun saveFor(accountKey: String, progress: PlayerProgress) {
        preferences.edit().putString(profileKey(accountKey), encode(progress)).apply()
    }

    private fun encode(progress: PlayerProgress): String = JSONObject()
        .put("accountKey", progress.accountKey)
        .put("displayName", progress.displayName)
        .put("totalExp", progress.totalExp)
        .put("stats", JSONObject()
            .put("totalMatches", progress.stats.totalMatches)
            .put("wins", progress.stats.wins)
            .put("totalRank", progress.stats.totalRank)
            .put("yajuTargets", progress.stats.yajuTargets)
            .put("yajuSuccesses", progress.stats.yajuSuccesses)
            .put("challengeMatches", progress.stats.challengeMatches)
            .put("challengeWins", progress.stats.challengeWins)
        )
        .toString()

    private fun decode(raw: String): PlayerProgress {
        val root = JSONObject(raw)
        val stats = root.optJSONObject("stats") ?: JSONObject()
        return PlayerProgress(
            accountKey = root.optString("accountKey", "guest"),
            displayName = root.optString("displayName", "GUEST"),
            totalExp = root.optInt("totalExp", 0),
            stats = MatchStats(
                totalMatches = stats.optInt("totalMatches", 0),
                wins = stats.optInt("wins", 0),
                totalRank = stats.optInt("totalRank", 0),
                yajuTargets = stats.optInt("yajuTargets", 0),
                yajuSuccesses = stats.optInt("yajuSuccesses", 0),
                challengeMatches = stats.optInt("challengeMatches", 0),
                challengeWins = stats.optInt("challengeWins", 0),
            ),
        )
    }

    private fun profileKey(accountKey: String) = "profile:$accountKey"
    private fun seenMatchesKey(accountKey: String) = "seen_matches:$accountKey"

    companion object {
        private const val KEY_ACTIVE_ACCOUNT = "active_account"
        private const val MAX_SEEN_MATCHES = 200
    }
}
