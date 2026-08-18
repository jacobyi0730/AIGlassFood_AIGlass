/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.microphone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

class AudioInputDeviceMonitor(private val context: Context) {
  private val audioManager = context.getSystemService(AudioManager::class.java)
  private var didChangeAudioMode = false
  private var previousAudioMode = AudioManager.MODE_NORMAL

  fun scan(): AudioInputScanResult {
    if (!hasBluetoothConnectPermission()) {
      return AudioInputScanResult(InputRouteStatus.PermissionRequired, emptyList())
    }

    val bluetoothDevices =
        try {
              audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            } catch (_: SecurityException) {
              return AudioInputScanResult(InputRouteStatus.PermissionRequired, emptyList())
            }
            .filter { it.isBluetoothAudioInput() }
            .map { device ->
              val name =
                  device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth mic"
              MicrophoneAudioDevice(
                  name = name,
                  typeLabel = device.typeLabel(),
                  isRayBanCandidate = name.isRayBanCandidate(),
              )
            }

    val routeStatus =
        when {
          bluetoothDevices.any { it.isRayBanCandidate } -> InputRouteStatus.RayBanCandidateFound
          bluetoothDevices.isNotEmpty() -> InputRouteStatus.BluetoothInputFound
          else -> InputRouteStatus.NoRayBanInput
        }

    return AudioInputScanResult(routeStatus, bluetoothDevices)
  }

  fun requestRayBanCommunicationRoute(): Boolean {
    if (!hasBluetoothConnectPermission()) {
      return false
    }

    val communicationDevice =
        try {
          audioManager.availableCommunicationDevices.firstOrNull { device ->
            device.isBluetoothCommunicationDevice() &&
                device.productName?.toString().orEmpty().isRayBanCandidate()
          }
        } catch (_: SecurityException) {
          return false
        }

    if (communicationDevice == null) {
      return false
    }

    previousAudioMode = audioManager.mode
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    didChangeAudioMode = true

    return try {
      audioManager.setCommunicationDevice(communicationDevice)
    } catch (_: SecurityException) {
      false
    }
  }

  fun releaseCommunicationRoute() {
    try {
      audioManager.clearCommunicationDevice()
    } catch (_: SecurityException) {
      // Ignore; the route will be reclaimed by the system.
    }
    if (didChangeAudioMode) {
      audioManager.mode = previousAudioMode
      didChangeAudioMode = false
    }
  }

  private fun hasBluetoothConnectPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
  }

  private fun AudioDeviceInfo.isBluetoothAudioInput(): Boolean {
    return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLE_HEADSET
  }

  private fun AudioDeviceInfo.isBluetoothCommunicationDevice(): Boolean {
    return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLE_HEADSET
  }

  private fun AudioDeviceInfo.typeLabel(): String {
    return when (type) {
      AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
      AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
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
