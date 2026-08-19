package com.mtvs.food.food.recipe

import android.net.Uri
import com.mtvs.food.microphone.InputRouteStatus
import com.mtvs.food.microphone.RecognitionStatus
import com.mtvs.food.speaker.AudioRouteStatus
import com.mtvs.food.speaker.TtsPlaybackStatus

data class FoodRecipeUiState(
    val isGlassesRegistered: Boolean = false,
    val hasActiveDevice: Boolean = false,
    val sessionStateText: String = "idle",
    val streamStateText: String = "stopped",
    val isCameraSessionActive: Boolean = false,
    val hasCameraSession: Boolean = false,
    val isStreaming: Boolean = false,
    val isBusy: Boolean = false,
    val hasReceivedPreviewFrame: Boolean = false,
    val isCapturingImage: Boolean = false,
    val capturedImageUri: Uri? = null,
    val promptText: String = "",
    val partialSttText: String = "",
    val finalSttText: String = "",
    val recognitionStatus: RecognitionStatus = RecognitionStatus.Idle,
    val inputRouteStatus: InputRouteStatus = InputRouteStatus.NoRayBanInput,
    val selectedInputDeviceName: String? = null,
    val isRequestingRecipe: Boolean = false,
    val requestReadyMessage: String? = null,
    val recipeText: String = "",
    val ttsPlaybackStatus: TtsPlaybackStatus = TtsPlaybackStatus.Initializing,
    val audioRouteStatus: AudioRouteStatus = AudioRouteStatus.NoBluetoothAudio,
    val selectedOutputDeviceName: String? = null,
    val isKoreanVoiceFallback: Boolean = false,
    val errorMessage: String? = null,
    val showCameraPermissionRedirectConfirm: Boolean = false,
) {
  val isListening: Boolean
    get() =
        recognitionStatus == RecognitionStatus.Preparing ||
            recognitionStatus == RecognitionStatus.Ready ||
            recognitionStatus == RecognitionStatus.Listening

  val isSpeakingRecipe: Boolean
    get() = ttsPlaybackStatus == TtsPlaybackStatus.Playing

  val canRequestRecipe: Boolean
    get() = capturedImageUri != null && promptText.isNotBlank() && !isRequestingRecipe
}
