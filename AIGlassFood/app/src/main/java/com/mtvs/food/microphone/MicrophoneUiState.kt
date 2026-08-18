/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.microphone

enum class InputRouteStatus {
  PermissionRequired,
  NoRayBanInput,
  BluetoothInputFound,
  RayBanCandidateFound,
}

enum class RecognitionStatus {
  Idle,
  Preparing,
  Ready,
  Listening,
  Completed,
  Error,
}

enum class MicrophoneError {
  RecordAudioPermissionRequired,
  BluetoothPermissionRequired,
  RayBanInputUnavailable,
  RayBanRouteUnavailable,
  SpeechRecognizerUnavailable,
  NoSpeechDetected,
  RecognitionFailed,
}

data class MicrophoneAudioDevice(
    val name: String,
    val typeLabel: String,
    val isRayBanCandidate: Boolean,
)

data class AudioInputScanResult(
    val routeStatus: InputRouteStatus,
    val devices: List<MicrophoneAudioDevice>,
)

data class MicrophoneUiState(
    val inputRouteStatus: InputRouteStatus = InputRouteStatus.NoRayBanInput,
    val recognitionStatus: RecognitionStatus = RecognitionStatus.Idle,
    val audioDevices: List<MicrophoneAudioDevice> = emptyList(),
    val selectedDeviceName: String? = null,
    val partialText: String = "",
    val finalText: String = "",
    val resultCandidates: List<String> = emptyList(),
    val lastError: MicrophoneError? = null,
) {
  val isListening: Boolean
    get() =
        recognitionStatus == RecognitionStatus.Preparing ||
            recognitionStatus == RecognitionStatus.Ready ||
            recognitionStatus == RecognitionStatus.Listening

  val hasResult: Boolean
    get() = partialText.isNotBlank() || finalText.isNotBlank() || resultCandidates.isNotEmpty()
}
