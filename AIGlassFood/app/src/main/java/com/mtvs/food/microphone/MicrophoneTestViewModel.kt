/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.microphone

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

class MicrophoneTestViewModel(application: Application) : AndroidViewModel(application) {
  private val audioInputDeviceMonitor = AudioInputDeviceMonitor(application)
  private val _uiState = MutableStateFlow(MicrophoneUiState())
  val uiState: StateFlow<MicrophoneUiState> = _uiState.asStateFlow()

  private val speechToTextController =
      SpeechToTextController(
          context = application,
          listener =
              object : SpeechToTextController.Listener {
                override fun onReady() {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(recognitionStatus = RecognitionStatus.Ready, lastError = null)
                    }
                  }
                }

                override fun onBeginningOfSpeech() {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(recognitionStatus = RecognitionStatus.Listening, lastError = null)
                    }
                  }
                }

                override fun onPartialResults(text: String) {
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(
                          recognitionStatus = RecognitionStatus.Listening,
                          partialText = text,
                          lastError = null,
                      )
                    }
                  }
                }

                override fun onFinalResults(results: List<String>) {
                  audioInputDeviceMonitor.releaseCommunicationRoute()
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(
                          recognitionStatus = RecognitionStatus.Completed,
                          finalText = results.firstOrNull().orEmpty(),
                          partialText = "",
                          resultCandidates = results.take(MAX_RESULT_CANDIDATES),
                          lastError = null,
                      )
                    }
                  }
                }

                override fun onError(error: MicrophoneError) {
                  audioInputDeviceMonitor.releaseCommunicationRoute()
                  viewModelScope.launch {
                    _uiState.update {
                      it.copy(recognitionStatus = RecognitionStatus.Error, lastError = error)
                    }
                  }
                }

                override fun onEndOfSpeech() {
                  viewModelScope.launch {
                    _uiState.update {
                      if (it.recognitionStatus == RecognitionStatus.Listening) {
                        it.copy(recognitionStatus = RecognitionStatus.Preparing)
                      } else {
                        it
                      }
                    }
                  }
                }
              },
      )

  init {
    refreshAudioDevices()
  }

  fun refreshAudioDevices() {
    val result = audioInputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          inputRouteStatus = result.routeStatus,
          audioDevices = result.devices,
          selectedDeviceName = result.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
          lastError =
              if (it.lastError == MicrophoneError.BluetoothPermissionRequired ||
                  it.lastError == MicrophoneError.RayBanInputUnavailable
              ) {
                null
              } else {
                it.lastError
              },
      )
    }
  }

  fun toggleRecognition(hasRecordAudioPermission: Boolean) {
    if (_uiState.value.isListening) {
      stopRecognition()
    } else {
      startRecognition(hasRecordAudioPermission)
    }
  }

  fun startRecognition(hasRecordAudioPermission: Boolean) {
    if (!hasRecordAudioPermission) {
      _uiState.update { it.copy(lastError = MicrophoneError.RecordAudioPermissionRequired) }
      return
    }

    if (!speechToTextController.isAvailable()) {
      _uiState.update {
        it.copy(
            recognitionStatus = RecognitionStatus.Error,
            lastError = MicrophoneError.SpeechRecognizerUnavailable,
        )
      }
      return
    }

    val scanResult = audioInputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          inputRouteStatus = scanResult.routeStatus,
          audioDevices = scanResult.devices,
          selectedDeviceName = scanResult.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
      )
    }

    when (scanResult.routeStatus) {
      InputRouteStatus.PermissionRequired -> {
        _uiState.update { it.copy(lastError = MicrophoneError.BluetoothPermissionRequired) }
        return
      }
      InputRouteStatus.NoRayBanInput,
      InputRouteStatus.BluetoothInputFound -> {
        _uiState.update { it.copy(lastError = MicrophoneError.RayBanInputUnavailable) }
        return
      }
      InputRouteStatus.RayBanCandidateFound -> Unit
    }

    if (!audioInputDeviceMonitor.requestRayBanCommunicationRoute()) {
      _uiState.update {
        it.copy(
            recognitionStatus = RecognitionStatus.Error,
            lastError = MicrophoneError.RayBanRouteUnavailable,
        )
      }
      return
    }

    _uiState.update {
      it.copy(
          recognitionStatus = RecognitionStatus.Preparing,
          partialText = "",
          finalText = "",
          resultCandidates = emptyList(),
          lastError = null,
      )
    }
    speechToTextController.start()
  }

  fun stopRecognition() {
    speechToTextController.stop()
    _uiState.update { it.copy(recognitionStatus = RecognitionStatus.Preparing) }
  }

  fun clearResult() {
    _uiState.update {
      it.copy(partialText = "", finalText = "", resultCandidates = emptyList(), lastError = null)
    }
  }

  fun cancelRecognition() {
    speechToTextController.cancel()
    audioInputDeviceMonitor.releaseCommunicationRoute()
    _uiState.update {
      it.copy(recognitionStatus = RecognitionStatus.Idle, partialText = "", lastError = null)
    }
  }

  override fun onCleared() {
    speechToTextController.destroy()
    audioInputDeviceMonitor.releaseCommunicationRoute()
    super.onCleared()
  }

  companion object {
    private const val MAX_RESULT_CANDIDATES = 3
  }

  class Factory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(MicrophoneTestViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        return MicrophoneTestViewModel(application) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
