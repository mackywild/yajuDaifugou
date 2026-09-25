package com.example.daifugo.android.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.daifugo.android.GameAudioCue

/**
 * 野獣ルール専用の差し替え式オーディオプレイヤー。
 *
 * app/src/main/res/raw/ に次のファイルを置くと有効になる。
 *
 * - yaju_available.mp3 / .wav : 「野獣上がりできます」用SE
 * - yaju_success.mp3 / .wav   : 「やりますねぇ」用SE
 * - yaju_bgm.mp3 / .wav       : 野獣対象者がいる間にループするBGM
 *
 * ファイルが存在しない場合は無音で処理を継続するため、
 * 音声未配置の状態でもアプリは通常動作する。
 */
object YajuAudioPlayer {

    private var bgmPlayer: MediaPlayer? = null

    /** カットイン等の短いSEを1回再生する。 */
    fun play(context: Context, cue: GameAudioCue) {
        val resourceName = when (cue) {
            GameAudioCue.YAJU_AVAILABLE -> "yaju_available"
            GameAudioCue.YAJU_SUCCESS -> "yaju_success"
        }

        val resourceId = rawResourceId(context, resourceName)
        if (resourceId == 0) {
            return
        }

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
     * 野獣上がり条件を獲得したプレイヤーがいる間のBGMを開始する。
     * 既に再生中なら先頭へ戻さず、そのまま継続する。
     */
    @Synchronized
    fun startBgm(context: Context) {
        if (bgmPlayer != null) {
            return
        }

        val resourceId = rawResourceId(context, "yaju_bgm")
        if (resourceId == 0) {
            return
        }

        val player = MediaPlayer.create(context.applicationContext, resourceId)
            ?: return

        player.isLooping = true
        // カットインSEを潰さないようBGMは少し下げて再生する。
        player.setVolume(0.55f, 0.55f)
        player.setOnErrorListener { failedPlayer, _, _ ->
            synchronized(this) {
                if (bgmPlayer === failedPlayer) {
                    bgmPlayer = null
                }
            }
            failedPlayer.release()
            true
        }

        bgmPlayer = player
        player.start()
    }

    /** 野獣BGMを停止し、MediaPlayerのリソースを解放する。 */
    @Synchronized
    fun stopBgm() {
        val player = bgmPlayer ?: return
        bgmPlayer = null

        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        player.release()
    }

    private fun rawResourceId(context: Context, resourceName: String): Int =
        context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName,
        )
}
