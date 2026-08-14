/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.speaker

enum class TtsPlaybackStatus {
  Initializing,
  Ready,
  Playing,
  Completed,
  Error,
}

enum class AudioRouteStatus {
  PermissionRequired,
  NoBluetoothAudio,
  BluetoothAudioFound,
  RayBanCandidateFound,
}

enum class SpeakerError {
  EmptyText,
  BluetoothPermissionRequired,
  BluetoothAudioUnavailable,
  TtsInitializationFailed,
  TtsSpeakFailed,
}

data class SpeakerAudioDevice(
    val name: String,
    val typeLabel: String,
    val isRayBanCandidate: Boolean,
)

data class AudioOutputScanResult(
    val routeStatus: AudioRouteStatus,
    val devices: List<SpeakerAudioDevice>,
)

data class SpeakerUiState(
    val inputText: String = "",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val playbackStatus: TtsPlaybackStatus = TtsPlaybackStatus.Initializing,
    val audioRouteStatus: AudioRouteStatus = AudioRouteStatus.NoBluetoothAudio,
    val audioDevices: List<SpeakerAudioDevice> = emptyList(),
    val selectedDeviceName: String? = null,
    val isKoreanVoiceFallback: Boolean = false,
    val lastError: SpeakerError? = null,
) {
  val canSpeak: Boolean
    get() = playbackStatus != TtsPlaybackStatus.Initializing && inputText.isNotBlank()

  val isPlaying: Boolean
    get() = playbackStatus == TtsPlaybackStatus.Playing
}
