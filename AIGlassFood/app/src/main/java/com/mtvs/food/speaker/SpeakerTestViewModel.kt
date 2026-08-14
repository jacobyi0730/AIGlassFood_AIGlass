/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.speaker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpeakerTestViewModel(application: Application) : AndroidViewModel(application) {
  private val audioOutputDeviceMonitor = AudioOutputDeviceMonitor(application)
  private val _uiState = MutableStateFlow(SpeakerUiState())
  val uiState: StateFlow<SpeakerUiState> = _uiState.asStateFlow()

  private val ttsController =
      TtsSpeakerController(
          context = application,
          listener =
              object : TtsSpeakerController.Listener {
                override fun onReady(isKoreanVoiceFallback: Boolean) {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(
                          playbackStatus = TtsPlaybackStatus.Ready,
                          isKoreanVoiceFallback = isKoreanVoiceFallback,
                          lastError = null,
                      )
                    }
                  }
                }

                override fun onStart() {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(playbackStatus = TtsPlaybackStatus.Playing, lastError = null)
                    }
                  }
                }

                override fun onDone() {
                  viewModelScope.launch {
                    _uiState.update { it.copy(playbackStatus = TtsPlaybackStatus.Completed) }
                  }
                }

                override fun onError() {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(
                          playbackStatus = TtsPlaybackStatus.Error,
                          lastError =
                              if (it.playbackStatus == TtsPlaybackStatus.Initializing) {
                                SpeakerError.TtsInitializationFailed
                              } else {
                                SpeakerError.TtsSpeakFailed
                              },
                      )
                    }
                  }
                }
              },
      )

  init {
    refreshAudioDevices()
  }

  fun updateText(text: String) {
    _uiState.update { it.copy(inputText = text.take(MAX_TEXT_LENGTH), lastError = null) }
  }

  fun useSampleText(text: String) {
    _uiState.update { it.copy(inputText = text.take(MAX_TEXT_LENGTH), lastError = null) }
  }

  fun updateSpeechRate(value: Float) {
    _uiState.update { it.copy(speechRate = value.coerceIn(MIN_SPEECH_VALUE, MAX_SPEECH_VALUE)) }
  }

  fun updatePitch(value: Float) {
    _uiState.update { it.copy(pitch = value.coerceIn(MIN_SPEECH_VALUE, MAX_SPEECH_VALUE)) }
  }

  fun refreshAudioDevices() {
    val result = audioOutputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          audioRouteStatus = result.routeStatus,
          audioDevices = result.devices,
          selectedDeviceName = result.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
          lastError =
              if (it.lastError == SpeakerError.BluetoothPermissionRequired ||
                  it.lastError == SpeakerError.BluetoothAudioUnavailable
              ) {
                null
              } else {
                it.lastError
              },
      )
    }
  }

  fun speak() {
    val current = _uiState.value
    if (current.inputText.isBlank()) {
      _uiState.update { it.copy(lastError = SpeakerError.EmptyText) }
      return
    }

    val scanResult = audioOutputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          audioRouteStatus = scanResult.routeStatus,
          audioDevices = scanResult.devices,
          selectedDeviceName = scanResult.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
      )
    }

    when (scanResult.routeStatus) {
      AudioRouteStatus.PermissionRequired -> {
        _uiState.update { it.copy(lastError = SpeakerError.BluetoothPermissionRequired) }
        return
      }
      AudioRouteStatus.NoBluetoothAudio -> {
        _uiState.update { it.copy(lastError = SpeakerError.BluetoothAudioUnavailable) }
        return
      }
      AudioRouteStatus.BluetoothAudioFound,
      AudioRouteStatus.RayBanCandidateFound -> Unit
    }

    val didStart =
        ttsController.speak(
            text = current.inputText.trim(),
            speechRate = current.speechRate,
            pitch = current.pitch,
        )
    if (!didStart) {
      _uiState.update {
        it.copy(playbackStatus = TtsPlaybackStatus.Error, lastError = SpeakerError.TtsSpeakFailed)
      }
    }
  }

  fun stop() {
    ttsController.stop()
    _uiState.update { it.copy(playbackStatus = TtsPlaybackStatus.Ready) }
  }

  override fun onCleared() {
    ttsController.shutdown()
    super.onCleared()
  }

  companion object {
    const val MAX_TEXT_LENGTH = 500
    const val MIN_SPEECH_VALUE = 0.5f
    const val MAX_SPEECH_VALUE = 2.0f
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(SpeakerTestViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        return SpeakerTestViewModel(application) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
