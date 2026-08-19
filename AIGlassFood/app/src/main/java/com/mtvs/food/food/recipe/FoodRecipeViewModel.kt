package com.mtvs.food.food.recipe

import android.app.Application
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.mtvs.food.camera.CameraViewModel
import com.mtvs.food.camera.CapturePreview
import com.mtvs.food.food.camera.FoodImageCacheRepository
import com.mtvs.food.microphone.AudioInputDeviceMonitor
import com.mtvs.food.microphone.InputRouteStatus
import com.mtvs.food.microphone.MicrophoneError
import com.mtvs.food.microphone.RecognitionStatus
import com.mtvs.food.microphone.SpeechToTextController
import com.mtvs.food.speaker.AudioOutputDeviceMonitor
import com.mtvs.food.speaker.AudioRouteStatus
import com.mtvs.food.speaker.SpeakerError
import com.mtvs.food.speaker.TtsPlaybackStatus
import com.mtvs.food.speaker.TtsSpeakerController
import com.mtvs.food.wearables.WearablesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FoodRecipeViewModel(
    application: Application,
    private val wearablesViewModel: WearablesViewModel,
) : AndroidViewModel(application) {
  private val cameraViewModel = CameraViewModel(application, wearablesViewModel)
  private val imageCacheRepository = FoodImageCacheRepository(application)
  private val audioInputDeviceMonitor = AudioInputDeviceMonitor(application)
  private val audioOutputDeviceMonitor = AudioOutputDeviceMonitor(application)

  private val _uiState = MutableStateFlow(FoodRecipeUiState())
  val uiState: StateFlow<FoodRecipeUiState> = _uiState.asStateFlow()

  private var lastCapturedPhotoId: Int? = null

  private val speechToTextController =
      SpeechToTextController(
          context = application,
          listener =
              object : SpeechToTextController.Listener {
                override fun onReady() {
                  _uiState.update {
                    it.copy(
                        recognitionStatus = RecognitionStatus.Ready,
                        errorMessage = null,
                        requestReadyMessage = null,
                    )
                  }
                }

                override fun onBeginningOfSpeech() {
                  _uiState.update {
                    it.copy(
                        recognitionStatus = RecognitionStatus.Listening,
                        errorMessage = null,
                        requestReadyMessage = null,
                    )
                  }
                }

                override fun onPartialResults(text: String) {
                  _uiState.update {
                    it.copy(
                        recognitionStatus = RecognitionStatus.Listening,
                        partialSttText = text,
                        errorMessage = null,
                    )
                  }
                }

                override fun onFinalResults(results: List<String>) {
                  audioInputDeviceMonitor.releaseCommunicationRoute()
                  val finalText = results.firstOrNull().orEmpty()
                  _uiState.update {
                    it.copy(
                        recognitionStatus = RecognitionStatus.Completed,
                        partialSttText = "",
                        finalSttText = finalText,
                        promptText = finalText,
                        recipeText = "",
                        isRequestingRecipe = false,
                        requestReadyMessage = null,
                        errorMessage = null,
                    )
                  }
                }

                override fun onError(error: MicrophoneError) {
                  audioInputDeviceMonitor.releaseCommunicationRoute()
                  _uiState.update {
                    it.copy(
                        recognitionStatus = RecognitionStatus.Error,
                        errorMessage = error.toMessage(),
                        requestReadyMessage = null,
                    )
                  }
                }

                override fun onEndOfSpeech() {
                  _uiState.update {
                    if (it.recognitionStatus == RecognitionStatus.Listening) {
                      it.copy(recognitionStatus = RecognitionStatus.Preparing)
                    } else {
                      it
                    }
                  }
                }
              },
      )

  private val ttsSpeakerController =
      TtsSpeakerController(
          context = application,
          listener =
              object : TtsSpeakerController.Listener {
                override fun onReady(isKoreanVoiceFallback: Boolean) {
                  _uiState.update {
                    it.copy(
                        ttsPlaybackStatus = TtsPlaybackStatus.Ready,
                        isKoreanVoiceFallback = isKoreanVoiceFallback,
                        errorMessage = null,
                    )
                  }
                }

                override fun onStart() {
                  _uiState.update {
                    it.copy(ttsPlaybackStatus = TtsPlaybackStatus.Playing, errorMessage = null)
                  }
                }

                override fun onDone() {
                  _uiState.update { it.copy(ttsPlaybackStatus = TtsPlaybackStatus.Completed) }
                }

                override fun onError() {
                  _uiState.update {
                    it.copy(
                        ttsPlaybackStatus = TtsPlaybackStatus.Error,
                        errorMessage = SpeakerError.TtsSpeakFailed.toMessage(),
                    )
                  }
                }
              },
      )

  init {
    refreshAudioInputState()
    refreshAudioOutputState()

    viewModelScope.launch {
      wearablesViewModel.uiState.collect { wearablesUi ->
        _uiState.update {
          it.copy(
              isGlassesRegistered = wearablesUi.isRegistered,
              hasActiveDevice = wearablesUi.hasActiveDevice,
          )
        }
      }
    }

    viewModelScope.launch {
      cameraViewModel.uiState.collect { cameraUi ->
        _uiState.update {
          it.copy(
              sessionStateText = cameraUi.sessionStateText,
              streamStateText = cameraUi.streamStateText,
              isCameraSessionActive = cameraUi.isSessionActive,
              hasCameraSession = cameraUi.hasSession,
              isStreaming = cameraUi.isStreaming,
              isBusy = cameraUi.isBusy,
              hasReceivedPreviewFrame = cameraUi.hasReceivedFirstFrame,
              isCapturingImage = cameraUi.isCapturingPhoto,
              showCameraPermissionRedirectConfirm = cameraUi.showCameraPermissionRedirectConfirm,
          )
        }

        val preview = cameraUi.activePreview
        if (preview is CapturePreview.Photo) {
          val captureId = System.identityHashCode(preview.bitmap)
          if (lastCapturedPhotoId != captureId) {
            lastCapturedPhotoId = captureId
            val imageUri = imageCacheRepository.saveCapturedPhoto(preview.bitmap)
            imageCacheRepository.delete(_uiState.value.capturedImageUri)
            _uiState.update {
              it.copy(
                  capturedImageUri = imageUri,
                  recipeText = "",
                  isRequestingRecipe = false,
                  requestReadyMessage = null,
                  errorMessage =
                      imageUri?.let { null } ?: "The food photo could not be saved. Try again.",
              )
            }
            cameraViewModel.dismissCapturePreview()
          }
        }
      }
    }
  }

  fun setSurface(surface: Surface?) {
    cameraViewModel.setSurface(surface)
  }

  fun startSession() {
    clearMessages()
    cameraViewModel.startSession()
  }

  fun endSession() {
    cameraViewModel.endSession()
  }

  fun startPreview() {
    clearMessages()
    cameraViewModel.startStreaming()
  }

  fun stopPreview() {
    cameraViewModel.stopStreaming()
  }

  fun captureImage() {
    clearMessages()
    cameraViewModel.capturePhoto()
  }

  fun retakeImage() {
    imageCacheRepository.delete(_uiState.value.capturedImageUri)
    lastCapturedPhotoId = null
    _uiState.update {
      it.copy(
          capturedImageUri = null,
          recipeText = "",
          isRequestingRecipe = false,
          requestReadyMessage = null,
          errorMessage = null,
      )
    }
  }

  fun confirmCameraPermissionRedirect(
      requestPermission: suspend (Permission) -> PermissionStatus
  ) {
    cameraViewModel.confirmCameraPermissionRedirect(requestPermission)
  }

  fun cancelCameraPermissionRedirect() {
    cameraViewModel.cancelCameraPermissionRedirect()
  }

  fun refreshAudioInputState() {
    val result = audioInputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          inputRouteStatus = result.routeStatus,
          selectedInputDeviceName =
              result.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
      )
    }
  }

  fun refreshAudioOutputState() {
    val result = audioOutputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          audioRouteStatus = result.routeStatus,
          selectedOutputDeviceName =
              result.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
      )
    }
  }

  fun startListening(requestRecordAudioPermission: suspend () -> Boolean) {
    viewModelScope.launch {
      clearMessages()
      val permissionGranted = requestRecordAudioPermission()
      if (!permissionGranted) {
        _uiState.update { it.copy(errorMessage = MicrophoneError.RecordAudioPermissionRequired.toMessage()) }
        return@launch
      }

      if (!speechToTextController.isAvailable()) {
        _uiState.update { it.copy(errorMessage = MicrophoneError.SpeechRecognizerUnavailable.toMessage()) }
        return@launch
      }

      val scanResult = audioInputDeviceMonitor.scan()
      _uiState.update {
        it.copy(
            inputRouteStatus = scanResult.routeStatus,
            selectedInputDeviceName =
                scanResult.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
        )
      }

      when (scanResult.routeStatus) {
        InputRouteStatus.PermissionRequired -> {
          _uiState.update { it.copy(errorMessage = MicrophoneError.BluetoothPermissionRequired.toMessage()) }
          return@launch
        }
        InputRouteStatus.NoRayBanInput,
        InputRouteStatus.BluetoothInputFound -> {
          _uiState.update { it.copy(errorMessage = MicrophoneError.RayBanInputUnavailable.toMessage()) }
          return@launch
        }
        InputRouteStatus.RayBanCandidateFound -> Unit
      }

      if (!audioInputDeviceMonitor.requestRayBanCommunicationRoute()) {
        _uiState.update { it.copy(errorMessage = MicrophoneError.RayBanRouteUnavailable.toMessage()) }
        return@launch
      }

      _uiState.update {
        it.copy(
            recognitionStatus = RecognitionStatus.Preparing,
            partialSttText = "",
            finalSttText = "",
            errorMessage = null,
            requestReadyMessage = null,
        )
      }
      speechToTextController.start()
    }
  }

  fun stopListening() {
    speechToTextController.stop()
    _uiState.update { it.copy(recognitionStatus = RecognitionStatus.Preparing) }
  }

  fun cancelListening() {
    speechToTextController.cancel()
    audioInputDeviceMonitor.releaseCommunicationRoute()
    _uiState.update {
      it.copy(
          recognitionStatus = RecognitionStatus.Idle,
          partialSttText = "",
          errorMessage = null,
          requestReadyMessage = null,
      )
    }
  }

  fun updatePromptText(text: String) {
    _uiState.update {
      it.copy(
          promptText = text,
          recipeText = "",
          isRequestingRecipe = false,
          requestReadyMessage = null,
          errorMessage = null,
      )
    }
  }

  fun clearPrompt() {
    _uiState.update {
      it.copy(
          promptText = "",
          partialSttText = "",
          finalSttText = "",
          recipeText = "",
          isRequestingRecipe = false,
          requestReadyMessage = null,
          errorMessage = null,
      )
    }
  }

  fun prepareRecipeRequest() {
    val imageUri = _uiState.value.capturedImageUri
    val promptText = _uiState.value.promptText.trim()

    when {
      imageUri == null -> {
        _uiState.update {
          it.copy(
              errorMessage = "Capture a food photo first.",
              requestReadyMessage = null,
              isRequestingRecipe = false,
          )
        }
      }
      promptText.isBlank() -> {
        _uiState.update {
          it.copy(
              errorMessage = "Speak or type what you want to cook first.",
              requestReadyMessage = null,
              isRequestingRecipe = false,
          )
        }
      }
      else -> {
        _uiState.update {
          it.copy(
              recipeText = "",
              errorMessage = null,
              isRequestingRecipe = false,
              requestReadyMessage = "The recipe request is ready to send.",
          )
        }
      }
    }
  }

  fun onRecipeRequestStarted() {
    _uiState.update {
      it.copy(
          isRequestingRecipe = true,
          recipeText = "",
          errorMessage = null,
          requestReadyMessage = null,
      )
    }
  }

  fun onRecipeRequestSucceeded(recipeText: String) {
    _uiState.update {
      it.copy(
          isRequestingRecipe = false,
          recipeText = recipeText,
          errorMessage = null,
          requestReadyMessage = null,
      )
    }
  }

  fun onRecipeRequestFailed(message: String) {
    _uiState.update {
      it.copy(
          isRequestingRecipe = false,
          errorMessage = message,
          requestReadyMessage = null,
      )
    }
  }

  fun speakRecipe() {
    val currentRecipe = _uiState.value.recipeText.trim()
    if (currentRecipe.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Generate a recipe before starting TTS.") }
      return
    }

    val scanResult = audioOutputDeviceMonitor.scan()
    _uiState.update {
      it.copy(
          audioRouteStatus = scanResult.routeStatus,
          selectedOutputDeviceName =
              scanResult.devices.firstOrNull { device -> device.isRayBanCandidate }?.name,
      )
    }

    when (scanResult.routeStatus) {
      AudioRouteStatus.PermissionRequired -> {
        _uiState.update { it.copy(errorMessage = SpeakerError.BluetoothPermissionRequired.toMessage()) }
        return
      }
      AudioRouteStatus.NoBluetoothAudio -> {
        _uiState.update { it.copy(errorMessage = SpeakerError.BluetoothAudioUnavailable.toMessage()) }
        return
      }
      AudioRouteStatus.BluetoothAudioFound,
      AudioRouteStatus.RayBanCandidateFound -> Unit
    }

    val didStart = ttsSpeakerController.speak(text = currentRecipe, speechRate = 1.0f, pitch = 1.0f)
    if (!didStart) {
      _uiState.update { it.copy(errorMessage = SpeakerError.TtsSpeakFailed.toMessage()) }
    }
  }

  fun stopRecipeSpeech() {
    ttsSpeakerController.stop()
    _uiState.update { it.copy(ttsPlaybackStatus = TtsPlaybackStatus.Ready) }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  private fun clearMessages() {
    _uiState.update { it.copy(errorMessage = null, requestReadyMessage = null) }
  }

  override fun onCleared() {
    imageCacheRepository.delete(_uiState.value.capturedImageUri)
    speechToTextController.destroy()
    audioInputDeviceMonitor.releaseCommunicationRoute()
    ttsSpeakerController.shutdown()
    cameraViewModel.release()
    super.onCleared()
  }

  class Factory(
      private val application: Application,
      private val wearablesViewModel: WearablesViewModel,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(FoodRecipeViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        return FoodRecipeViewModel(application, wearablesViewModel) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}

private fun MicrophoneError.toMessage(): String {
  return when (this) {
    MicrophoneError.RecordAudioPermissionRequired -> "Microphone permission is required."
    MicrophoneError.BluetoothPermissionRequired -> "Bluetooth permission is required."
    MicrophoneError.RayBanInputUnavailable -> "Ray-Ban Meta microphone input was not found."
    MicrophoneError.RayBanRouteUnavailable -> "The Ray-Ban Meta microphone route is unavailable."
    MicrophoneError.SpeechRecognizerUnavailable -> "Speech recognition is unavailable on this device."
    MicrophoneError.NoSpeechDetected -> "No speech was detected."
    MicrophoneError.RecognitionFailed -> "Speech recognition failed. Try again."
  }
}

private fun SpeakerError.toMessage(): String {
  return when (this) {
    SpeakerError.EmptyText -> "Enter text before starting TTS."
    SpeakerError.BluetoothPermissionRequired -> "Bluetooth permission is required."
    SpeakerError.BluetoothAudioUnavailable -> "Select Ray-Ban Meta as the Android audio output."
    SpeakerError.TtsInitializationFailed -> "Text to speech could not be initialized."
    SpeakerError.TtsSpeakFailed -> "Text to speech could not start."
  }
}
