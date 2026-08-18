/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtvs.food.R
import com.mtvs.food.microphone.InputRouteStatus
import com.mtvs.food.microphone.MicrophoneError
import com.mtvs.food.microphone.MicrophoneTestViewModel
import com.mtvs.food.microphone.RecognitionStatus
import kotlinx.coroutines.launch

@Composable
fun MicrophoneTestScreen(
    onBack: () -> Unit,
    onRequestRecordAudioPermission: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    testModeSwitch: (@Composable () -> Unit)? = null,
    viewModel: MicrophoneTestViewModel =
        viewModel(
            factory =
                MicrophoneTestViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val ui by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .statusBarsPadding()
              .navigationBarsPadding()
              .verticalScroll(rememberScrollState())
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    MicrophoneHeader(
        onBack = {
          viewModel.cancelRecognition()
          onBack()
        },
        onRefresh = viewModel::refreshAudioDevices,
    )
    testModeSwitch?.invoke()

    MicrophoneStatusPanel(
        routeStatus = ui.inputRouteStatus,
        recognitionStatus = ui.recognitionStatus,
        selectedDeviceName = ui.selectedDeviceName,
        lastError = ui.lastError,
    )

    Button(
        onClick = {
          scope.launch {
            val hasPermission = onRequestRecordAudioPermission()
            viewModel.toggleRecognition(hasPermission)
          }
        },
        modifier = Modifier.fillMaxWidth().height(58.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (ui.isListening) AppColor.RecordAccent else AppColor.DeepBlue
            ),
    ) {
      Icon(
          imageVector = if (ui.isListening) Icons.Default.Stop else Icons.Default.Mic,
          contentDescription = null,
      )
      Text(
          text =
              if (ui.isListening) stringResource(R.string.microphone_stop_button)
              else stringResource(R.string.microphone_start_button),
          modifier = Modifier.padding(start = 8.dp),
          fontWeight = FontWeight.SemiBold,
      )
    }

    MicrophoneResultPanel(
        title = stringResource(R.string.microphone_partial_result_title),
        text =
            ui.partialText.ifBlank {
              if (ui.isListening) stringResource(R.string.microphone_listening_placeholder)
              else stringResource(R.string.microphone_empty_partial_result)
            },
        isMuted = ui.partialText.isBlank(),
    )

    MicrophoneResultPanel(
        title = stringResource(R.string.microphone_final_result_title),
        text = ui.finalText.ifBlank { stringResource(R.string.microphone_empty_final_result) },
        isMuted = ui.finalText.isBlank(),
        minHeight = 150.dp,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
          onClick = {
            copyText(context, ui.finalText.ifBlank { ui.partialText })
          },
          enabled = ui.hasResult,
          modifier = Modifier.weight(1f).height(50.dp),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null)
        Text(stringResource(R.string.microphone_copy_button), modifier = Modifier.padding(start = 8.dp))
      }
      OutlinedButton(
          onClick = viewModel::clearResult,
          enabled = ui.hasResult,
          modifier = Modifier.weight(1f).height(50.dp),
      ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Text(stringResource(R.string.microphone_clear_button), modifier = Modifier.padding(start = 8.dp))
      }
    }

    if (ui.resultCandidates.size > 1) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.microphone_candidates_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        ui.resultCandidates.forEachIndexed { index, candidate ->
          Text(
              text = stringResource(R.string.microphone_candidate_item, index + 1, candidate),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.DarkGray,
          )
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(
          onClick = {
            openMicrophoneSettings(context = context, action = Settings.ACTION_BLUETOOTH_SETTINGS)
          },
          modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Icon(Icons.Default.BluetoothAudio, contentDescription = null)
        Text(
            stringResource(R.string.microphone_open_bluetooth_settings),
            modifier = Modifier.padding(start = 8.dp),
        )
      }
      OutlinedButton(
          onClick = { openAppSettings(context) },
          modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Text(
            stringResource(R.string.microphone_open_app_settings),
            modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun MicrophoneHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    IconButton(onClick = onBack) {
      Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
    }
    Icon(Icons.Default.Mic, contentDescription = null, tint = AppColor.DeepBlue)
    Text(
        text = stringResource(R.string.microphone_test_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
    )
    IconButton(onClick = onRefresh) {
      Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.microphone_refresh))
    }
  }
}

@Composable
private fun MicrophoneStatusPanel(
    routeStatus: InputRouteStatus,
    recognitionStatus: RecognitionStatus,
    selectedDeviceName: String?,
    lastError: MicrophoneError?,
) {
  val statusText =
      when {
        lastError != null -> lastError.asText()
        recognitionStatus == RecognitionStatus.Preparing ->
            stringResource(R.string.microphone_status_preparing)
        recognitionStatus == RecognitionStatus.Ready -> stringResource(R.string.microphone_status_ready)
        recognitionStatus == RecognitionStatus.Listening ->
            stringResource(R.string.microphone_status_listening)
        recognitionStatus == RecognitionStatus.Completed ->
            stringResource(R.string.microphone_status_completed)
        routeStatus == InputRouteStatus.RayBanCandidateFound && selectedDeviceName != null ->
            stringResource(R.string.microphone_status_rayban_found, selectedDeviceName)
        routeStatus == InputRouteStatus.BluetoothInputFound ->
            stringResource(R.string.microphone_status_bluetooth_found)
        routeStatus == InputRouteStatus.PermissionRequired ->
            stringResource(R.string.microphone_error_bluetooth_permission)
        else -> stringResource(R.string.microphone_status_no_rayban)
      }

  val icon =
      when {
        lastError != null -> Icons.Default.Warning
        recognitionStatus == RecognitionStatus.Listening -> Icons.Default.Mic
        routeStatus == InputRouteStatus.RayBanCandidateFound -> Icons.Default.CheckCircle
        routeStatus == InputRouteStatus.NoRayBanInput -> Icons.Default.MicOff
        else -> Icons.Default.BluetoothAudio
      }
  val tint =
      when {
        lastError != null || routeStatus == InputRouteStatus.NoRayBanInput -> AppColor.Yellow
        routeStatus == InputRouteStatus.RayBanCandidateFound -> AppColor.Green
        else -> AppColor.DeepBlue
      }

  Surface(
      color = Color(0xFFF5F7FA),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(icon, contentDescription = null, tint = tint)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(statusText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text = stringResource(R.string.microphone_route_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
        )
      }
    }
  }
}

@Composable
private fun MicrophoneResultPanel(
    title: String,
    text: String,
    isMuted: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 100.dp,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Surface(
        color = Color(0xFFF5F7FA),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
    ) {
      Text(
          text = text,
          modifier = Modifier.padding(14.dp),
          style = MaterialTheme.typography.bodyLarge,
          color = if (isMuted) Color.Gray else Color.Black,
      )
    }
  }
}

@Composable
private fun MicrophoneError.asText(): String {
  return when (this) {
    MicrophoneError.RecordAudioPermissionRequired ->
        stringResource(R.string.microphone_error_record_audio_permission)
    MicrophoneError.BluetoothPermissionRequired ->
        stringResource(R.string.microphone_error_bluetooth_permission)
    MicrophoneError.RayBanInputUnavailable ->
        stringResource(R.string.microphone_error_rayban_unavailable)
    MicrophoneError.RayBanRouteUnavailable ->
        stringResource(R.string.microphone_error_rayban_route)
    MicrophoneError.SpeechRecognizerUnavailable ->
        stringResource(R.string.microphone_error_stt_unavailable)
    MicrophoneError.NoSpeechDetected -> stringResource(R.string.microphone_error_no_speech)
    MicrophoneError.RecognitionFailed -> stringResource(R.string.microphone_error_recognition)
  }
}

private fun copyText(context: Context, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText("STT result", text))
}

private fun openMicrophoneSettings(context: Context, action: String) {
  try {
    context.startActivity(Intent(action))
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS))
  }
}

private fun openAppSettings(context: Context) {
  val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
  try {
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS))
  }
}
