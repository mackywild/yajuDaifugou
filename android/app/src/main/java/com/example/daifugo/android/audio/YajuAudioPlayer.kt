package com.example.daifugo.android.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.daifugo.android.GameAudioCue

/**
 * 野獣大富豪のBGM / SEプレイヤー。
 *
 * app/src/main/res/raw/ に置く差し替え式音源:
 *
 * - menu_bgm.mp3 / .wav      : タイトル〜メニュー〜ゲーム開始前の共通ループBGM
 * - yaju_available.mp3 / .wav: 「野獣上がりできます」用SE
 * - yaju_success.mp3 / .wav  : 「やりますねぇ」用SE
 * - yaju_bgm.mp3 / .wav      : 野獣対象者がいる対戦中のループBGM
 *
 * menu_bgm は約2分のループ素材を想定するが、MediaPlayer側は音源長を問わず
 * 繰り返す。素材未配置時は無音で通常動作する。
 */
object YajuAudioPlayer {

    private var menuBgmPlayer: MediaPlayer? = null
    private var yajuBgmPlayer: MediaPlayer? = null

    fun play(context: Context, cue: GameAudioCue) {
        val resourceName = when (cue) {
            GameAudioCue.YAJU_AVAILABLE -> "yaju_available"
            GameAudioCue.YAJU_SUCCESS -> "yaju_success"
        }

        val resourceId = rawResourceId(context, resourceName)
        if (resourceId == 0) return

        MediaPlayer.create(context.applicationContext, resourceId)?.apply {
            setOnCompletionListener { player -> player.release() }
            setOnErrorListener { player, _, _ ->
                player.release()
                true
            }
            start()
        }
    }

    /**
     * タイトル〜ゲーム開始前で共有するラウンジBGM。
     * 画面遷移では作り直さず、再生位置を維持する。
     */
    @Synchronized
    fun startMenuBgm(context: Context) {
        if (menuBgmPlayer != null) return

        val resourceId = rawResourceId(context, "menu_bgm")
        if (resourceId == 0) return

        val player = MediaPlayer.create(context.applicationContext, resourceId) ?: return
        player.isLooping = true
        player.setVolume(0.42f, 0.42f)
        player.setOnErrorListener { failedPlayer, _, _ ->
            synchronized(this) {
                if (menuBgmPlayer === failedPlayer) menuBgmPlayer = null
            }
            failedPlayer.release()
            true
        }
        menuBgmPlayer = player
        player.start()
    }

    @Synchronized
    fun stopMenuBgm() {
        release(menuBgmPlayer)
        menuBgmPlayer = null
    }

    /**
     * 野獣対象者がいる対戦中の専用BGM。
     * 二重再生を防ぐためラウンジBGMを止めてから開始する。
     */
    @Synchronized
    fun startBgm(context: Context) {
        stopMenuBgm()
        if (yajuBgmPlayer != null) return

        val resourceId = rawResourceId(context, "yaju_bgm")
        if (resourceId == 0) return

        val player = MediaPlayer.create(context.applicationContext, resourceId) ?: return
        player.isLooping = true
        player.setVolume(0.55f, 0.55f)
        player.setOnErrorListener { failedPlayer, _, _ ->
            synchronized(this) {
                if (yajuBgmPlayer === failedPlayer) yajuBgmPlayer = null
            }
            failedPlayer.release()
            true
        }
        yajuBgmPlayer = player
        player.start()
    }

    @Synchronized
    fun stopBgm() {
        release(yajuBgmPlayer)
        yajuBgmPlayer = null
    }

    @Synchronized
    fun stopAllBgm() {
        release(menuBgmPlayer)
        release(yajuBgmPlayer)
        menuBgmPlayer = null
        yajuBgmPlayer = null
    }

    private fun release(player: MediaPlayer?) {
        player ?: return
        runCatching {
            if (player.isPlaying) player.stop()
        }
        player.release()
    }

    private fun rawResourceId(context: Context, resourceName: String): Int =
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
}
