/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtvs.food.R
import com.mtvs.food.speaker.AudioRouteStatus
import com.mtvs.food.speaker.SpeakerError
import com.mtvs.food.speaker.SpeakerTestViewModel
import com.mtvs.food.speaker.TtsPlaybackStatus
import java.util.Locale

@Composable
fun SpeakerTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    testModeSwitch: (@Composable () -> Unit)? = null,
    viewModel: SpeakerTestViewModel =
        viewModel(
            factory =
                SpeakerTestViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application
                )
        ),
) {
  val ui by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val sampleTexts =
      listOf(
          stringResource(R.string.speaker_sample_text_short),
          stringResource(R.string.speaker_sample_text_success),
          stringResource(R.string.speaker_sample_text_noise),
      )

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
    SpeakerHeader(onBack = onBack, onRefresh = viewModel::refreshAudioDevices)
    testModeSwitch?.invoke()

    SpeakerStatusPanel(
        routeStatus = ui.audioRouteStatus,
        playbackStatus = ui.playbackStatus,
        selectedDeviceName = ui.selectedDeviceName,
        hasKoreanVoiceFallback = ui.isKoreanVoiceFallback,
        lastError = ui.lastError,
    )

    OutlinedTextField(
        value = ui.inputText,
        onValueChange = viewModel::updateText,
        modifier = Modifier.fillMaxWidth().height(150.dp),
        label = { Text(stringResource(R.string.speaker_input_label)) },
        placeholder = { Text(stringResource(R.string.speaker_input_placeholder)) },
        supportingText = {
          Text(
              text =
                  stringResource(
                      R.string.speaker_character_count,
                      ui.inputText.length,
                      SpeakerTestViewModel.MAX_TEXT_LENGTH,
                  )
          )
        },
        maxLines = 6,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      Button(
          onClick = viewModel::speak,
          enabled = ui.canSpeak && !ui.isPlaying,
          modifier = Modifier.weight(1f).height(52.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AppColor.DeepBlue),
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Text(stringResource(R.string.speaker_read_button), modifier = Modifier.padding(start = 8.dp))
      }
      OutlinedButton(
          onClick = viewModel::stop,
          enabled = ui.isPlaying,
          modifier = Modifier.weight(1f).height(52.dp),
      ) {
        Icon(Icons.Default.Stop, contentDescription = null)
        Text(stringResource(R.string.speaker_stop_button), modifier = Modifier.padding(start = 8.dp))
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
          text = stringResource(R.string.speaker_samples_title),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      sampleTexts.forEach { sample ->
        OutlinedButton(
            onClick = { viewModel.useSampleText(sample) },
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(sample, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    SpeakerSlider(
        title = stringResource(R.string.speaker_rate_label),
        value = ui.speechRate,
        onValueChange = viewModel::updateSpeechRate,
    )
    SpeakerSlider(
        title = stringResource(R.string.speaker_pitch_label),
        value = ui.pitch,
        onValueChange = viewModel::updatePitch,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(
          onClick = {
            openSettings(context = context, action = Settings.ACTION_BLUETOOTH_SETTINGS)
          },
          modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Icon(Icons.Default.BluetoothAudio, contentDescription = null)
        Text(
            stringResource(R.string.speaker_open_bluetooth_settings),
            modifier = Modifier.padding(start = 8.dp),
        )
      }
      OutlinedButton(
          onClick = { openSettings(context = context, action = Settings.ACTION_SOUND_SETTINGS) },
          modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Text(
            stringResource(R.string.speaker_open_sound_settings),
            modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun SpeakerHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    IconButton(onClick = onBack) {
      Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
    }
    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AppColor.DeepBlue)
    Text(
        text = stringResource(R.string.speaker_test_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
    )
    IconButton(onClick = onRefresh) {
      Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.speaker_refresh))
    }
  }
}

@Composable
private fun SpeakerStatusPanel(
    routeStatus: AudioRouteStatus,
    playbackStatus: TtsPlaybackStatus,
    selectedDeviceName: String?,
    hasKoreanVoiceFallback: Boolean,
    lastError: SpeakerError?,
) {
  val statusText =
      when {
        lastError != null -> lastError.asText()
        playbackStatus == TtsPlaybackStatus.Initializing ->
            stringResource(R.string.speaker_status_tts_initializing)
        playbackStatus == TtsPlaybackStatus.Playing -> stringResource(R.string.speaker_status_playing)
        playbackStatus == TtsPlaybackStatus.Completed ->
            stringResource(R.string.speaker_status_completed)
        routeStatus == AudioRouteStatus.RayBanCandidateFound && selectedDeviceName != null ->
            stringResource(R.string.speaker_status_rayban_found, selectedDeviceName)
        routeStatus == AudioRouteStatus.BluetoothAudioFound ->
            stringResource(R.string.speaker_status_bluetooth_found)
        routeStatus == AudioRouteStatus.PermissionRequired ->
            stringResource(R.string.speaker_error_bluetooth_permission)
        else -> stringResource(R.string.speaker_status_no_bluetooth)
      }

  val icon =
      when {
        lastError != null -> Icons.Default.Warning
        playbackStatus == TtsPlaybackStatus.Playing -> Icons.Default.VolumeUp
        routeStatus == AudioRouteStatus.RayBanCandidateFound -> Icons.Default.CheckCircle
        else -> Icons.Default.BluetoothAudio
      }
  val tint =
      when {
        lastError != null || routeStatus == AudioRouteStatus.NoBluetoothAudio -> AppColor.Yellow
        routeStatus == AudioRouteStatus.RayBanCandidateFound -> AppColor.Green
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
            text = stringResource(R.string.speaker_route_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray,
        )
        if (hasKoreanVoiceFallback) {
          Text(
              text = stringResource(R.string.speaker_korean_fallback),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.DarkGray,
          )
        }
      }
    }
  }
}

@Composable
private fun SpeakerSlider(title: String, value: Float, onValueChange: (Float) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
      Text(String.format(Locale.getDefault(), "%.1fx", value), color = Color.DarkGray)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = SpeakerTestViewModel.MIN_SPEECH_VALUE..SpeakerTestViewModel.MAX_SPEECH_VALUE,
    )
  }
}

@Composable
private fun SpeakerError.asText(): String {
  return when (this) {
    SpeakerError.EmptyText -> stringResource(R.string.speaker_error_empty_text)
    SpeakerError.BluetoothPermissionRequired ->
        stringResource(R.string.speaker_error_bluetooth_permission)
    SpeakerError.BluetoothAudioUnavailable -> stringResource(R.string.speaker_error_no_bluetooth)
    SpeakerError.TtsInitializationFailed -> stringResource(R.string.speaker_error_tts_init)
    SpeakerError.TtsSpeakFailed -> stringResource(R.string.speaker_error_tts_speak)
  }
}

private fun openSettings(context: android.content.Context, action: String) {
  try {
    context.startActivity(Intent(action))
  } catch (_: ActivityNotFoundException) {
    context.startActivity(Intent(Settings.ACTION_SETTINGS))
  }
}
