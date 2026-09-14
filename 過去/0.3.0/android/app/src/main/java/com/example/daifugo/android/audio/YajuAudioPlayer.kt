package com.example.daifugo.android.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.daifugo.android.GameAudioCue

/**
 * 野獣ルール専用の差し替え式音声プレイヤー。
 *
 * 音声ファイルそのものはリポジトリへ同梱しない。
 * 次のファイルを app/src/main/res/raw/ へ置いてAPKを再ビルドするだけで有効になる。
 *
 * - yaju_available.mp3 / .wav : 「野獣上がりできます」用
 * - yaju_success.mp3 / .wav   : 「やりますねぇ」用
 *
 * ファイルが存在しない場合は無音で処理を継続するため、
 * 音声未配置の状態でもアプリは通常動作する。
 */
object YajuAudioPlayer {

    fun play(context: Context, cue: GameAudioCue) {
        val resourceName = when (cue) {
            GameAudioCue.YAJU_AVAILABLE -> "yaju_available"
            GameAudioCue.YAJU_SUCCESS -> "yaju_success"
        }

        val resourceId = context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName,
        )

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
}
