/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.speaker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

class AudioOutputDeviceMonitor(private val context: Context) {
  private val audioManager = context.getSystemService(AudioManager::class.java)

  fun scan(): AudioOutputScanResult {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED
    ) {
      return AudioOutputScanResult(AudioRouteStatus.PermissionRequired, emptyList())
    }

    val devices =
        try {
          audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: SecurityException) {
          return AudioOutputScanResult(AudioRouteStatus.PermissionRequired, emptyList())
        }

    val bluetoothDevices =
        devices
            .filter { it.isBluetoothAudioOutput() }
            .map { device ->
              val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth"
              SpeakerAudioDevice(
                  name = name,
                  typeLabel = device.typeLabel(),
                  isRayBanCandidate = name.isRayBanCandidate(),
              )
            }

    val routeStatus =
        when {
          bluetoothDevices.any { it.isRayBanCandidate } -> AudioRouteStatus.RayBanCandidateFound
          bluetoothDevices.isNotEmpty() -> AudioRouteStatus.BluetoothAudioFound
          else -> AudioRouteStatus.NoBluetoothAudio
        }

    return AudioOutputScanResult(routeStatus, bluetoothDevices)
  }

  private fun AudioDeviceInfo.isBluetoothAudioOutput(): Boolean {
    return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
        type == AudioDeviceInfo.TYPE_BLE_SPEAKER
  }

  private fun AudioDeviceInfo.typeLabel(): String {
    return when (type) {
      AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
      AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
      AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
      AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE Speaker"
      else -> "Bluetooth"
    }
  }

  private fun String.isRayBanCandidate(): Boolean {
    val normalized = lowercase()
    return normalized.contains("ray-ban") ||
        normalized.contains("rayban") ||
        normalized.contains("meta") ||
        normalized.contains("glasses")
  }
}
