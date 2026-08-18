package com.program.braintrainer.util

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Pušta kratak zvučni efekat i OSLOBAĐA MediaPlayer kada se reprodukcija završi.
 *
 * Ranije se na tri mesta zvalo `MediaPlayer.create(...).start()` bez `release()`, pa je
 * svaki odigran zvuk ostavljao neoslobođen native resurs (MediaPlayer/AudioTrack).
 */
fun playSound(context: Context, @RawRes resId: Int) {
    val player = MediaPlayer.create(context.applicationContext, resId) ?: return
    player.setOnCompletionListener { it.release() }
    player.setOnErrorListener { mp, _, _ ->
        mp.release()
        true
    }
    player.start()
}
