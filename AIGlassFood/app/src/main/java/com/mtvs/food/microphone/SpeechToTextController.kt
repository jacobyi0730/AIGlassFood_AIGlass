/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.microphone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechToTextController(
    private val context: Context,
    private val listener: Listener,
) {
  interface Listener {
    fun onReady()

    fun onBeginningOfSpeech()

    fun onPartialResults(text: String)

    fun onFinalResults(results: List<String>)

    fun onError(error: MicrophoneError)

    fun onEndOfSpeech()
  }

  private var speechRecognizer: SpeechRecognizer? = null

  fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

  fun start() {
    if (!isAvailable()) {
      listener.onError(MicrophoneError.SpeechRecognizerUnavailable)
      return
    }

    val recognizer =
        speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
          speechRecognizer = it
        }
    recognizer.setRecognitionListener(createRecognitionListener())
    recognizer.startListening(createRecognizerIntent())
  }

  fun stop() {
    speechRecognizer?.stopListening()
  }

  fun cancel() {
    speechRecognizer?.cancel()
  }

  fun destroy() {
    speechRecognizer?.destroy()
    speechRecognizer = null
  }

  private fun createRecognizerIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
  }

  private fun createRecognitionListener(): RecognitionListener {
    return object : RecognitionListener {
      override fun onReadyForSpeech(params: Bundle?) {
        listener.onReady()
      }

      override fun onBeginningOfSpeech() {
        listener.onBeginningOfSpeech()
      }

      override fun onRmsChanged(rmsdB: Float) = Unit

      override fun onBufferReceived(buffer: ByteArray?) = Unit

      override fun onEndOfSpeech() {
        listener.onEndOfSpeech()
      }

      override fun onError(error: Int) {
        listener.onError(error.toMicrophoneError())
      }

      override fun onResults(results: Bundle?) {
        val recognized =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        if (recognized.isEmpty()) {
          listener.onError(MicrophoneError.NoSpeechDetected)
        } else {
          listener.onFinalResults(recognized)
        }
      }

      override fun onPartialResults(partialResults: Bundle?) {
        val text =
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
        if (text.isNotBlank()) {
          listener.onPartialResults(text)
        }
      }

      override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
  }

  private fun Int.toMicrophoneError(): MicrophoneError {
    return when (this) {
      SpeechRecognizer.ERROR_NO_MATCH,
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> MicrophoneError.NoSpeechDetected
      else -> MicrophoneError.RecognitionFailed
    }
  }
}
