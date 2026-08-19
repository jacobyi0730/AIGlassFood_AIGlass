package com.mtvs.food.food.ui

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.mtvs.food.R
import com.mtvs.food.food.recipe.FoodRecipeUiState
import com.mtvs.food.food.recipe.FoodRecipeViewModel
import com.mtvs.food.microphone.InputRouteStatus
import com.mtvs.food.microphone.RecognitionStatus
import com.mtvs.food.speaker.AudioRouteStatus
import com.mtvs.food.speaker.TtsPlaybackStatus
import com.mtvs.food.ui.AppColor
import com.mtvs.food.ui.SwitchButton
import com.mtvs.food.wearables.WearablesViewModel

private enum class BannerTone {
  Info,
  Success,
  Warning,
  Error,
}

private enum class SupportAction {
  OpenBluetoothSettings,
  OpenAppSettings,
}

@Composable
fun FoodRecipeScreen(
    wearablesViewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    onRequestRecordAudioPermission: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    foodRecipeViewModel: FoodRecipeViewModel = viewModel(
        factory =
            FoodRecipeViewModel.Factory(
                application = (LocalActivity.current as ComponentActivity).application,
                wearablesViewModel = wearablesViewModel,
            ),
    ),
) {
  val uiState by foodRecipeViewModel.uiState.collectAsStateWithLifecycle()
  FoodRecipeContent(
      uiState = uiState,
      modifier = modifier,
      onSurfaceChanged = foodRecipeViewModel::setSurface,
      onStartSession = foodRecipeViewModel::startSession,
      onEndSession = foodRecipeViewModel::endSession,
      onStartPreview = foodRecipeViewModel::startPreview,
      onStopPreview = foodRecipeViewModel::stopPreview,
      onCaptureImage = foodRecipeViewModel::captureImage,
      onRetakeImage = foodRecipeViewModel::retakeImage,
      onPromptChanged = foodRecipeViewModel::updatePromptText,
      onStartListening = { foodRecipeViewModel.startListening(onRequestRecordAudioPermission) },
      onStopListening = foodRecipeViewModel::stopListening,
      onClearPrompt = foodRecipeViewModel::clearPrompt,
      onPrepareRecipeRequest = foodRecipeViewModel::prepareRecipeRequest,
      onSpeakRecipe = foodRecipeViewModel::speakRecipe,
      onStopRecipeSpeech = foodRecipeViewModel::stopRecipeSpeech,
  )

  if (uiState.showCameraPermissionRedirectConfirm) {
    AlertDialog(
        onDismissRequest = foodRecipeViewModel::cancelCameraPermissionRedirect,
        title = { Text(stringResource(R.string.camera_permission_redirect_title)) },
        text = { Text(stringResource(R.string.camera_permission_redirect_message)) },
        confirmButton = {
          TextButton(
              onClick = {
                foodRecipeViewModel.confirmCameraPermissionRedirect(onRequestWearablesPermission)
              }
          ) {
            Text(stringResource(R.string.camera_permission_continue))
          }
        },
        dismissButton = {
          TextButton(onClick = foodRecipeViewModel::cancelCameraPermissionRedirect) {
            Text(stringResource(R.string.camera_permission_cancel))
          }
        },
    )
  }
}

@Composable
internal fun FoodRecipeContent(
    uiState: FoodRecipeUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onCaptureImage: () -> Unit,
    onRetakeImage: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearPrompt: () -> Unit,
    onPrepareRecipeRequest: () -> Unit,
    onSpeakRecipe: () -> Unit,
    onStopRecipeSpeech: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      HeroSection(uiState = uiState)
      StatusOverview(uiState = uiState)
      MessageBanner(
          uiState = uiState,
          onOpenBluetoothSettings = { context.openBluetoothSettings() },
          onOpenAppSettings = { context.openAppSettings() },
      )
      CameraSection(
          uiState = uiState,
          onSurfaceChanged = onSurfaceChanged,
          onStartSession = onStartSession,
          onEndSession = onEndSession,
          onStartPreview = onStartPreview,
          onStopPreview = onStopPreview,
          onCaptureImage = onCaptureImage,
          onRetakeImage = onRetakeImage,
      )
      PromptSection(
          uiState = uiState,
          onPromptChanged = onPromptChanged,
          onStartListening = onStartListening,
          onStopListening = onStopListening,
          onClearPrompt = onClearPrompt,
      )
      RequestSection(
          uiState = uiState,
          onPrepareRecipeRequest = onPrepareRecipeRequest,
      )
      ResultSection(
          uiState = uiState,
          onSpeakRecipe = onSpeakRecipe,
          onStopRecipeSpeech = onStopRecipeSpeech,
      )
    }
  }
}

@Composable
private fun HeroSection(uiState: FoodRecipeUiState) {
  val subtitle =
      when {
        !uiState.isGlassesRegistered -> stringResource(R.string.food_hero_subtitle_unregistered)
        uiState.isRequestingRecipe -> stringResource(R.string.food_hero_subtitle_loading)
        uiState.recipeText.isNotBlank() -> stringResource(R.string.food_hero_subtitle_result_ready)
        else -> stringResource(R.string.food_hero_subtitle_default)
      }

  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      color = Color.Transparent,
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0C3D91), Color(0xFF178A6D), Color(0xFFF3C25B))
                    )
                )
                .padding(20.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.food_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
        )
      }
    }
  }
}

@Composable
private fun StatusOverview(uiState: FoodRecipeUiState) {
  SectionCard(
      title = stringResource(R.string.food_status_title),
      bodyDescription = stringResource(R.string.food_status_description),
  ) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      StatusPill(
          label = stringResource(R.string.food_status_registration),
          value =
              if (uiState.isGlassesRegistered) {
                stringResource(R.string.food_status_registered)
              } else {
                stringResource(R.string.food_status_not_registered)
              },
          active = uiState.isGlassesRegistered,
      )
      StatusPill(
          label = stringResource(R.string.food_status_active_device),
          value =
              if (uiState.hasActiveDevice) {
                stringResource(R.string.food_status_connected)
              } else {
                stringResource(R.string.food_status_waiting)
              },
          active = uiState.hasActiveDevice,
      )
      StatusPill(
          label = stringResource(R.string.food_status_session),
          value = uiState.sessionStateText,
          active = uiState.isCameraSessionActive,
      )
      StatusPill(
          label = stringResource(R.string.food_status_stream),
          value = uiState.streamStateText,
          active = uiState.isStreaming,
      )
      StatusPill(
          label = stringResource(R.string.food_status_input),
          value = uiState.selectedInputDeviceName ?: uiState.inputRouteStatus.label(),
          active = uiState.inputRouteStatus == InputRouteStatus.RayBanCandidateFound,
      )
      StatusPill(
          label = stringResource(R.string.food_status_output),
          value = uiState.selectedOutputDeviceName ?: uiState.audioRouteStatus.label(),
          active = uiState.audioRouteStatus == AudioRouteStatus.RayBanCandidateFound,
      )
    }
  }
}

@Composable
private fun MessageBanner(
    uiState: FoodRecipeUiState,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
  val messageTone =
      when {
        uiState.errorMessage != null -> BannerTone.Error
        uiState.isRequestingRecipe -> BannerTone.Info
        uiState.requestReadyMessage != null -> BannerTone.Success
        !uiState.isGlassesRegistered -> BannerTone.Warning
        else -> null
      }

  val message =
      when {
        uiState.errorMessage != null -> uiState.errorMessage
        uiState.isRequestingRecipe -> stringResource(R.string.food_request_loading_message)
        uiState.requestReadyMessage != null -> uiState.requestReadyMessage
        !uiState.isGlassesRegistered -> stringResource(R.string.food_registration_help)
        else -> null
      }

  if (messageTone == null || message == null) return

  val actions = uiState.supportActions()

  BannerCard(
      tone = messageTone,
      message = message,
      actions = actions,
      onOpenBluetoothSettings = onOpenBluetoothSettings,
      onOpenAppSettings = onOpenAppSettings,
  )
}

@Composable
private fun CameraSection(
    uiState: FoodRecipeUiState,
    onSurfaceChanged: (android.view.Surface?) -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onStartPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onCaptureImage: () -> Unit,
    onRetakeImage: () -> Unit,
) {
  val previewContentDescription = stringResource(R.string.food_camera_preview_content_description)
  SectionCard(
      title = stringResource(R.string.food_camera_title),
      bodyDescription = stringResource(R.string.food_camera_description),
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(232.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .semantics { contentDescription = previewContentDescription },
        contentAlignment = Alignment.Center,
    ) {
      when {
        uiState.capturedImageUri != null -> CapturedImage(uri = uiState.capturedImageUri)
        uiState.hasCameraSession -> {
          AndroidExternalSurface(modifier = Modifier.fillMaxSize()) {
            onSurface { surface, _, _ ->
              onSurfaceChanged(surface)
              surface.onDestroyed { onSurfaceChanged(null) }
            }
          }
          if (uiState.isBusy || (uiState.isStreaming && !uiState.hasReceivedPreviewFrame)) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.wrapContentHeight(),
            ) {
              Row(
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.food_camera_loading),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }
        else -> {
          Text(
              text = stringResource(R.string.food_camera_placeholder),
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SwitchButton(
          label =
              if (uiState.hasCameraSession) {
                stringResource(R.string.end_session_button)
              } else {
                stringResource(R.string.start_session_button)
              },
          onClick = { if (uiState.hasCameraSession) onEndSession() else onStartSession() },
          modifier = Modifier.weight(1f).testTag("food_session_button"),
          isDestructive = uiState.hasCameraSession,
          enabled =
              if (uiState.hasCameraSession) {
                !uiState.isBusy
              } else {
                !uiState.isBusy && uiState.hasActiveDevice
              },
      )
      SwitchButton(
          label =
              if (uiState.isStreaming) {
                stringResource(R.string.stop_preview)
              } else {
                stringResource(R.string.start_preview)
              },
          onClick = { if (uiState.isStreaming) onStopPreview() else onStartPreview() },
          modifier = Modifier.weight(1f).testTag("food_preview_button"),
          enabled = uiState.hasCameraSession && !uiState.isBusy,
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SwitchButton(
          label = stringResource(R.string.food_capture_button),
          onClick = onCaptureImage,
          modifier = Modifier.weight(1f).testTag("food_capture_button"),
          enabled = uiState.isStreaming && !uiState.isCapturingImage,
      )
      SwitchButton(
          label = stringResource(R.string.food_retake_button),
          onClick = onRetakeImage,
          modifier = Modifier.weight(1f).testTag("food_retake_button"),
          enabled = uiState.capturedImageUri != null,
      )
    }
  }
}

@Composable
private fun PromptSection(
    uiState: FoodRecipeUiState,
    onPromptChanged: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClearPrompt: () -> Unit,
) {
  SectionCard(
      title = stringResource(R.string.food_prompt_title),
      bodyDescription = stringResource(R.string.food_prompt_description),
  ) {
    StatusPill(
        label = stringResource(R.string.food_prompt_listening_status),
        value = uiState.recognitionStatus.label(),
        active = uiState.isListening,
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SwitchButton(
          label =
              if (uiState.isListening) {
                stringResource(R.string.microphone_stop_button)
              } else {
                stringResource(R.string.microphone_start_button)
              },
          onClick = { if (uiState.isListening) onStopListening() else onStartListening() },
          modifier = Modifier.weight(1f).testTag("food_stt_toggle_button"),
      )
      SwitchButton(
          label = stringResource(R.string.food_clear_button),
          onClick = onClearPrompt,
          modifier = Modifier.weight(1f).testTag("food_clear_prompt_button"),
      )
    }

    if (uiState.partialSttText.isNotBlank()) {
      Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFFEAF2FF),
      ) {
        Text(
            text =
                stringResource(R.string.food_partial_prompt_format, uiState.partialSttText),
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColor.DeepBlue,
        )
      }
    }

    OutlinedTextField(
        value = uiState.promptText,
        onValueChange = onPromptChanged,
        modifier = Modifier.fillMaxWidth().testTag("food_prompt_field"),
        minLines = 5,
        label = { Text(stringResource(R.string.food_prompt_field_label)) },
        placeholder = { Text(stringResource(R.string.food_prompt_placeholder)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
  }
}

@Composable
private fun RequestSection(
    uiState: FoodRecipeUiState,
    onPrepareRecipeRequest: () -> Unit,
) {
  SectionCard(
      title = stringResource(R.string.food_request_title),
      bodyDescription = stringResource(R.string.food_request_description),
  ) {
    SwitchButton(
        label = stringResource(R.string.food_prepare_request_button),
        onClick = onPrepareRecipeRequest,
        modifier = Modifier.testTag("food_prepare_request_button"),
        enabled = uiState.canRequestRecipe,
    )

    if (uiState.isRequestingRecipe) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = AppColor.DeepBlue,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.food_request_loading_message),
            style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun ResultSection(
    uiState: FoodRecipeUiState,
    onSpeakRecipe: () -> Unit,
    onStopRecipeSpeech: () -> Unit,
) {
  SectionCard(
      title = stringResource(R.string.food_result_title),
      bodyDescription = stringResource(R.string.food_result_description),
  ) {
    StatusPill(
        label = stringResource(R.string.food_result_tts_status),
        value = uiState.ttsPlaybackStatus.label(),
        active = uiState.isSpeakingRecipe,
    )

    OutlinedTextField(
        value = uiState.recipeText,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth().testTag("food_recipe_result"),
        readOnly = true,
        minLines = 8,
        placeholder = { Text(stringResource(R.string.food_result_placeholder)) },
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      SwitchButton(
          label = stringResource(R.string.food_recipe_tts_button),
          onClick = onSpeakRecipe,
          modifier = Modifier.weight(1f).testTag("food_recipe_tts_button"),
          enabled = uiState.recipeText.isNotBlank() && !uiState.isSpeakingRecipe,
      )
      SwitchButton(
          label = stringResource(R.string.speaker_stop_button),
          onClick = onStopRecipeSpeech,
          modifier = Modifier.weight(1f).testTag("food_stop_tts_button"),
          enabled = uiState.isSpeakingRecipe,
      )
    }
  }
}

@Composable
private fun BannerCard(
    tone: BannerTone,
    message: String,
    actions: List<SupportAction>,
    onOpenBluetoothSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
  val (backgroundColor, foregroundColor) =
      when (tone) {
        BannerTone.Info -> Color(0xFFEAF2FF) to AppColor.DeepBlue
        BannerTone.Success -> Color(0xFFE8F6EA) to Color(0xFF276738)
        BannerTone.Warning -> Color(0xFFFFF5E0) to Color(0xFF8A4B00)
        BannerTone.Error -> Color(0xFFFDEBEC) to Color(0xFFAA071E)
      }

  Surface(
      modifier = Modifier.fillMaxWidth().testTag("food_message_banner"),
      shape = RoundedCornerShape(20.dp),
      color = backgroundColor,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = message,
          color = foregroundColor,
          style = MaterialTheme.typography.bodyMedium,
      )

      if (actions.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          actions.forEach { action ->
            TextButton(
                onClick =
                    when (action) {
                      SupportAction.OpenBluetoothSettings -> onOpenBluetoothSettings
                      SupportAction.OpenAppSettings -> onOpenAppSettings
                    },
                modifier =
                    Modifier.background(
                        color = Color.White.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(999.dp),
                    ),
            ) {
              Text(
                  text =
                      when (action) {
                        SupportAction.OpenBluetoothSettings ->
                            stringResource(R.string.food_open_bluetooth_settings)
                        SupportAction.OpenAppSettings ->
                            stringResource(R.string.food_open_app_settings)
                      },
                  color = foregroundColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    active: Boolean,
) {
  Surface(
      shape = RoundedCornerShape(999.dp),
      color = if (active) Color(0xFFEAF2FF) else Color(0xFFF4F5F6),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
          modifier =
              Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(if (active) AppColor.Green else Color.Gray)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
          text = "$label: $value",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF24303F),
      )
    }
  }
}

@Composable
private fun CapturedImage(uri: Uri) {
  val context = LocalContext.current
  val imageBitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
    value =
        runCatching {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    .asImageBitmap()
              } else {
                null
              }
            }
            .getOrNull()
  }

  if (imageBitmap != null) {
    Image(
        bitmap = imageBitmap!!,
        contentDescription = stringResource(R.string.captured_photo),
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
  } else {
    CircularProgressIndicator(color = Color.White)
  }
}

@Composable
private fun SectionCard(
    title: String,
    bodyDescription: String,
    content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = bodyDescription,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
      }
      content()
    }
  }
}

private fun FoodRecipeUiState.supportActions(): List<SupportAction> {
  val actions = linkedSetOf<SupportAction>()

  if (inputRouteStatus == InputRouteStatus.PermissionRequired ||
      audioRouteStatus == AudioRouteStatus.PermissionRequired) {
    actions += SupportAction.OpenAppSettings
  }

  if (inputRouteStatus == InputRouteStatus.NoRayBanInput ||
      inputRouteStatus == InputRouteStatus.BluetoothInputFound ||
      audioRouteStatus == AudioRouteStatus.NoBluetoothAudio ||
      audioRouteStatus == AudioRouteStatus.BluetoothAudioFound) {
    actions += SupportAction.OpenBluetoothSettings
  }

  return actions.toList()
}

private fun InputRouteStatus.label(): String {
  return when (this) {
    InputRouteStatus.PermissionRequired -> "Permission needed"
    InputRouteStatus.NoRayBanInput -> "Not detected"
    InputRouteStatus.BluetoothInputFound -> "Other Bluetooth mic"
    InputRouteStatus.RayBanCandidateFound -> "Ready"
  }
}

private fun AudioRouteStatus.label(): String {
  return when (this) {
    AudioRouteStatus.PermissionRequired -> "Permission needed"
    AudioRouteStatus.NoBluetoothAudio -> "Not detected"
    AudioRouteStatus.BluetoothAudioFound -> "Other Bluetooth audio"
    AudioRouteStatus.RayBanCandidateFound -> "Ready"
  }
}

private fun RecognitionStatus.label(): String {
  return when (this) {
    RecognitionStatus.Idle -> "Idle"
    RecognitionStatus.Preparing -> "Preparing"
    RecognitionStatus.Ready -> "Ready"
    RecognitionStatus.Listening -> "Listening"
    RecognitionStatus.Completed -> "Completed"
    RecognitionStatus.Error -> "Error"
  }
}

private fun TtsPlaybackStatus.label(): String {
  return when (this) {
    TtsPlaybackStatus.Initializing -> "Initializing"
    TtsPlaybackStatus.Ready -> "Ready"
    TtsPlaybackStatus.Playing -> "Playing"
    TtsPlaybackStatus.Completed -> "Completed"
    TtsPlaybackStatus.Error -> "Error"
  }
}

private fun android.content.Context.openBluetoothSettings() {
  startActivity(
      Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
  )
}

private fun android.content.Context.openAppSettings() {
  startActivity(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
  )
}
