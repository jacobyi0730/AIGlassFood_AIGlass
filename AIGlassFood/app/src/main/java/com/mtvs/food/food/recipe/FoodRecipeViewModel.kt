package com.mtvs.food.food.recipe

import android.app.Application
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.mtvs.food.BuildConfig
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
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FoodRecipeViewModel(
    application: Application,
    private val wearablesViewModel: WearablesViewModel,
) : AndroidViewModel(application) {
  private val logTag = "FoodRecipeFlow"
  private val cameraViewModel = CameraViewModel(application, wearablesViewModel)
  private val imageCacheRepository = FoodImageCacheRepository(application)
  private val audioInputDeviceMonitor = AudioInputDeviceMonitor(application)
  private val audioOutputDeviceMonitor = AudioOutputDeviceMonitor(application)
  private val recipeRepository = RecipeRepository(application.contentResolver)

  private val _uiState =
      MutableStateFlow(FoodRecipeUiState(serverBaseUrl = BuildConfig.FOOD_SERVER_BASE_URL))
  val uiState: StateFlow<FoodRecipeUiState> = _uiState.asStateFlow()

  private var lastCapturedPhotoId: Int? = null
  private var recipeRequestJob: Job? = null

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
                  cancelRecipeRequest()
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
            cancelRecipeRequest()
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
    cancelRecipeRequest()
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
        _uiState.update {
          it.copy(errorMessage = MicrophoneError.RecordAudioPermissionRequired.toMessage())
        }
        return@launch
      }

      if (!speechToTextController.isAvailable()) {
        _uiState.update {
          it.copy(errorMessage = MicrophoneError.SpeechRecognizerUnavailable.toMessage())
        }
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
          _uiState.update {
            it.copy(errorMessage = MicrophoneError.BluetoothPermissionRequired.toMessage())
          }
          return@launch
        }
        InputRouteStatus.NoRayBanInput,
        InputRouteStatus.BluetoothInputFound -> {
          _uiState.update {
            it.copy(errorMessage = MicrophoneError.RayBanInputUnavailable.toMessage())
          }
          return@launch
        }
        InputRouteStatus.RayBanCandidateFound -> Unit
      }

      if (!audioInputDeviceMonitor.requestRayBanCommunicationRoute()) {
        _uiState.update {
          it.copy(errorMessage = MicrophoneError.RayBanRouteUnavailable.toMessage())
        }
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
    cancelRecipeRequest()
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
    cancelRecipeRequest()
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

    Log.d(
        logTag,
        "prepareRecipeRequest imageUri=$imageUri promptLength=${promptText.length} serverBaseUrl=${BuildConfig.FOOD_SERVER_BASE_URL}"
    )

    when {
      imageUri == null -> {
        Log.w(logTag, "prepareRecipeRequest blocked: image is missing")
        _uiState.update {
          it.copy(
              errorMessage = "Capture a food photo first.",
              requestReadyMessage = null,
              isRequestingRecipe = false,
          )
        }
      }
      promptText.isBlank() -> {
        Log.w(logTag, "prepareRecipeRequest blocked: prompt is blank")
        _uiState.update {
          it.copy(
              errorMessage = "Speak or type what you want to cook first.",
              requestReadyMessage = null,
              isRequestingRecipe = false,
          )
        }
      }
      else -> requestRecipe(imageUri = imageUri, promptText = promptText)
    }
  }

  private fun requestRecipe(imageUri: Uri, promptText: String) {
    if (recipeRequestJob?.isActive == true) {
      Log.w(logTag, "requestRecipe skipped because another request is already running")
      return
    }

    recipeRequestJob =
        viewModelScope.launch {
          Log.d(logTag, "requestRecipe started imageUri=$imageUri")
          onRecipeRequestStarted()

          try {
            val response =
                recipeRepository.requestRecipe(
                    imageUri = imageUri,
                    prompt = promptText,
                    language = DEFAULT_RECIPE_LANGUAGE,
                )
            Log.d(
                logTag,
                "requestRecipe succeeded recipeLength=${response.recipeText.length} elapsedMs=${response.elapsedMs}"
            )
            onRecipeRequestSucceeded(response.recipeText)
          } catch (error: RecipeApiException) {
            Log.e(
                logTag,
                "requestRecipe failed kind=${error.kind} serverCode=${error.serverCode} message=${error.userMessage}",
                error,
            )
            if (error.kind == RecipeApiErrorKind.Cancelled) {
              _uiState.update {
                it.copy(
                    isRequestingRecipe = false,
                    requestReadyMessage = null,
                )
              }
            } else {
              onRecipeRequestFailed(error.userMessage)
            }
          } catch (_: IOException) {
            Log.e(
                logTag,
                "requestRecipe failed with generic IOException while reaching ${BuildConfig.FOOD_SERVER_BASE_URL}"
            )
            onRecipeRequestFailed(
                "The Android app could not reach the PC server. Check the server IP, port, and that both devices are on the same Wi-Fi network."
            )
          } finally {
            recipeRequestJob = null
          }
        }
  }

  private fun onRecipeRequestStarted() {
    _uiState.update {
      it.copy(
          isRequestingRecipe = true,
          recipeText = "",
          errorMessage = null,
          requestReadyMessage = "Requesting a recipe from ${BuildConfig.FOOD_SERVER_BASE_URL}",
      )
    }
  }

  private fun onRecipeRequestSucceeded(recipeText: String) {
    _uiState.update {
      it.copy(
          isRequestingRecipe = false,
          recipeText = recipeText,
          errorMessage = null,
          requestReadyMessage = "Recipe response received from the PC server.",
      )
    }
  }

  private fun onRecipeRequestFailed(message: String) {
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
        _uiState.update {
          it.copy(errorMessage = SpeakerError.BluetoothPermissionRequired.toMessage())
        }
        return
      }
      AudioRouteStatus.NoBluetoothAudio -> {
        _uiState.update {
          it.copy(errorMessage = SpeakerError.BluetoothAudioUnavailable.toMessage())
        }
        return
      }
      AudioRouteStatus.BluetoothAudioFound,
      AudioRouteStatus.RayBanCandidateFound -> Unit
    }

    val didStart =
        ttsSpeakerController.speak(text = currentRecipe, speechRate = 1.0f, pitch = 1.0f)
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

  private fun cancelRecipeRequest() {
    recipeRequestJob?.cancel()
    recipeRequestJob = null
  }

  private fun clearMessages() {
    _uiState.update { it.copy(errorMessage = null, requestReadyMessage = null) }
  }

  override fun onCleared() {
    cancelRecipeRequest()
    imageCacheRepository.delete(_uiState.value.capturedImageUri)
    speechToTextController.destroy()
    audioInputDeviceMonitor.releaseCommunicationRoute()
    ttsSpeakerController.shutdown()
    cameraViewModel.release()
    super.onCleared()
  }

  companion object {
    private const val DEFAULT_RECIPE_LANGUAGE = "ko-KR"
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
