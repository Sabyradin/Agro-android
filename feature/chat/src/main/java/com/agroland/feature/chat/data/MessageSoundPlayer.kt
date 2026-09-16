package com.agroland.feature.chat.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кіріс хабарлама дыбысы (Flutter MessageSoundPlayer): синглтон, volume 0.6,
 * AssetSource('sounds/message.mp3') → res/raw/message.mp3. Дыбыс ТЕК бөлек
 * бөлмеден келген хабарламаға ойналады (ашық бөлмеде қолданушы көріп тұр).
 * Әрқашан үнсіз сәтсіз (Flutter ReleaseMode.stop паритеті).
 */
@Singleton
class MessageSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var player: MediaPlayer? = null

    @Synchronized
    fun play() {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                context.resources.openRawResourceFd(R_RAW_MESSAGE).use { fd ->
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
                setVolume(0.6f, 0.6f)
                setOnCompletionListener { it.release() }
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Үнсіз қалдыру — дыбыс ешқашан қателік көрсетпейді (Flutter паритеті).
        }
    }

    private companion object {
        val R_RAW_MESSAGE = com.agroland.feature.chat.R.raw.message
    }
}