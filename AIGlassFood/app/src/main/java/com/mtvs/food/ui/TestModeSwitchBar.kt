/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.mtvs.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtvs.food.R

enum class AIGlassTestMode {
  Camera,
  Speaker,
  Microphone,
}

@Composable
fun TestModeSwitchBar(
    selectedMode: AIGlassTestMode,
    onModeSelected: (AIGlassTestMode) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
  val containerColor = if (dark) Color.White.copy(alpha = 0.14f) else Color(0xFFF5F7FA)
  val selectedColor = if (dark) Color.White else AppColor.DeepBlue
  val selectedContentColor = if (dark) Color.Black else Color.White
  val unselectedContentColor = if (dark) Color.White.copy(alpha = 0.82f) else Color.DarkGray

  Surface(
      modifier = modifier.fillMaxWidth(),
      color = containerColor,
      shape = RoundedCornerShape(8.dp),
  ) {
    Row(
        modifier = Modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      TestModeSwitchItem(
          label = stringResource(R.string.test_mode_camera),
          icon = Icons.Default.CameraAlt,
          selected = selectedMode == AIGlassTestMode.Camera,
          selectedColor = selectedColor,
          selectedContentColor = selectedContentColor,
          unselectedContentColor = unselectedContentColor,
          onClick = { onModeSelected(AIGlassTestMode.Camera) },
          modifier = Modifier.weight(1f),
      )
      TestModeSwitchItem(
          label = stringResource(R.string.test_mode_speaker),
          icon = Icons.Default.VolumeUp,
          selected = selectedMode == AIGlassTestMode.Speaker,
          selectedColor = selectedColor,
          selectedContentColor = selectedContentColor,
          unselectedContentColor = unselectedContentColor,
          onClick = { onModeSelected(AIGlassTestMode.Speaker) },
          modifier = Modifier.weight(1f),
      )
      TestModeSwitchItem(
          label = stringResource(R.string.test_mode_microphone),
          icon = Icons.Default.Mic,
          selected = selectedMode == AIGlassTestMode.Microphone,
          selectedColor = selectedColor,
          selectedContentColor = selectedContentColor,
          unselectedContentColor = unselectedContentColor,
          onClick = { onModeSelected(AIGlassTestMode.Microphone) },
          modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun TestModeSwitchItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val backgroundColor = if (selected) selectedColor else Color.Transparent
  val contentColor = if (selected) selectedContentColor else unselectedContentColor

  Row(
      modifier =
          modifier
              .height(42.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(backgroundColor)
              .clickable(onClick = onClick)
              .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = contentColor)
    Text(
        text = label,
        modifier = Modifier.padding(start = 6.dp),
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
  }
}
