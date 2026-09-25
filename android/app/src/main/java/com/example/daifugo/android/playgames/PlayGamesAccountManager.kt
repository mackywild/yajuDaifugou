package com.example.daifugo.android.playgames

import android.app.Activity
import android.content.Context
import com.example.daifugo.android.R
import com.google.android.gms.games.PlayGames

data class PlayGamesAccount(
    val configured: Boolean,
    val authenticated: Boolean,
    val playerId: String? = null,
    val displayName: String? = null,
    val errorMessage: String? = null,
)

class PlayGamesAccountManager(private val activity: Activity) {

    fun refresh(onResult: (PlayGamesAccount) -> Unit) {
        if (!isConfigured(activity)) {
            onResult(PlayGamesAccount(configured = false, authenticated = false))
            return
        }

        PlayGames.getGamesSignInClient(activity)
            .isAuthenticated
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.isAuthenticated) {
                    loadPlayer(onResult)
                } else {
                    onResult(PlayGamesAccount(configured = true, authenticated = false))
                }
            }
    }

    fun signIn(onResult: (PlayGamesAccount) -> Unit) {
        if (!isConfigured(activity)) {
            onResult(
                PlayGamesAccount(
                    configured = false,
                    authenticated = false,
                    errorMessage = "Play GamesのプロジェクトIDが未設定です",
                )
            )
            return
        }

        PlayGames.getGamesSignInClient(activity)
            .signIn()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.isAuthenticated) {
                    loadPlayer(onResult)
                } else {
                    onResult(
                        PlayGamesAccount(
                            configured = true,
                            authenticated = false,
                            errorMessage = task.exception?.message ?: "Play Gamesへのログインに失敗しました",
                        )
                    )
                }
            }
    }

    private fun loadPlayer(onResult: (PlayGamesAccount) -> Unit) {
        PlayGames.getPlayersClient(activity)
            .currentPlayer
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val player = task.result
                    onResult(
                        PlayGamesAccount(
                            configured = true,
                            authenticated = true,
                            playerId = player.playerId,
                            displayName = player.displayName,
                        )
                    )
                } else {
                    onResult(
                        PlayGamesAccount(
                            configured = true,
                            authenticated = true,
                            errorMessage = task.exception?.message ?: "プレイヤー情報を取得できませんでした",
                        )
                    )
                }
            }
    }

    companion object {
        fun isConfigured(context: Context): Boolean {
            val projectId = context.getString(R.string.game_services_project_id).trim()
            return projectId.isNotEmpty() && projectId != "0"
        }
    }
}
