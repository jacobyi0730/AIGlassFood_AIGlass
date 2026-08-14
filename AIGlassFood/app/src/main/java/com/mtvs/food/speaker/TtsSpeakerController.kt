/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.speaker

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsSpeakerController(
    context: Context,
    private val listener: Listener,
) : TextToSpeech.OnInitListener {
  interface Listener {
    fun onReady(isKoreanVoiceFallback: Boolean)

    fun onStart()

    fun onDone()

    fun onError()
  }

  private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)

  override fun onInit(status: Int) {
    val tts = textToSpeech
    if (status != TextToSpeech.SUCCESS || tts == null) {
      listener.onError()
      return
    }

    tts.setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    )
    tts.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            listener.onStart()
          }

          override fun onDone(utteranceId: String?) {
            listener.onDone()
          }

          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            listener.onError()
          }

          override fun onError(utteranceId: String?, errorCode: Int) {
            listener.onError()
          }
        }
    )

    val koreanResult = tts.setLanguage(Locale.KOREAN)
    val isKoreanUnsupported =
        koreanResult == TextToSpeech.LANG_MISSING_DATA ||
            koreanResult == TextToSpeech.LANG_NOT_SUPPORTED
    if (isKoreanUnsupported) {
      tts.setLanguage(Locale.getDefault())
    }
    listener.onReady(isKoreanUnsupported)
  }

  fun speak(text: String, speechRate: Float, pitch: Float): Boolean {
    val tts = textToSpeech ?: return false
    val utteranceId = "speaker-test-${System.currentTimeMillis()}"
    val params =
        Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }

    tts.stop()
    tts.setSpeechRate(speechRate)
    tts.setPitch(pitch)
    return tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId) == TextToSpeech.SUCCESS
  }

  fun stop() {
    textToSpeech?.stop()
  }

  fun shutdown() {
    textToSpeech?.stop()
    textToSpeech?.shutdown()
    textToSpeech = null
  }
}
