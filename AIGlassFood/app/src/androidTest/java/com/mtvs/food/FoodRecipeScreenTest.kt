package com.mtvs.food

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.mtvs.food.food.recipe.FoodRecipeUiState
import com.mtvs.food.food.ui.FoodRecipeContent
import com.mtvs.food.microphone.InputRouteStatus
import com.mtvs.food.microphone.RecognitionStatus
import com.mtvs.food.speaker.AudioRouteStatus
import com.mtvs.food.speaker.TtsPlaybackStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FoodRecipeScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun requestButtonDisabledUntilImageAndPromptAreReady() {
    composeTestRule.setContent {
      FoodRecipeContent(
          uiState = FoodRecipeUiState(),
          onSurfaceChanged = {},
          onStartSession = {},
          onEndSession = {},
          onStartPreview = {},
          onStopPreview = {},
          onCaptureImage = {},
          onRetakeImage = {},
          onPromptChanged = {},
          onStartListening = {},
          onStopListening = {},
          onClearPrompt = {},
          onPrepareRecipeRequest = {},
          onSpeakRecipe = {},
          onStopRecipeSpeech = {},
      )
    }

    composeTestRule.onNodeWithTag("food_prepare_request_button").assertIsNotEnabled()
  }

  @Test
  fun requestButtonEnabledWhenPromptAndImageExist() {
    composeTestRule.setContent {
      FoodRecipeContent(
          uiState =
              FoodRecipeUiState(
                  promptText = "Healthy dinner",
                  capturedImageUri = android.net.Uri.parse("content://food/image"),
              ),
          onSurfaceChanged = {},
          onStartSession = {},
          onEndSession = {},
          onStartPreview = {},
          onStopPreview = {},
          onCaptureImage = {},
          onRetakeImage = {},
          onPromptChanged = {},
          onStartListening = {},
          onStopListening = {},
          onClearPrompt = {},
          onPrepareRecipeRequest = {},
          onSpeakRecipe = {},
          onStopRecipeSpeech = {},
      )
    }

    composeTestRule.onNodeWithTag("food_prepare_request_button").assertIsEnabled()
  }

  @Test
  fun bluetoothHelpActionShownWhenRayBanMicIsUnavailable() {
    composeTestRule.setContent {
      FoodRecipeContent(
          uiState =
              FoodRecipeUiState(
                  errorMessage = "Ray-Ban Meta microphone input was not found.",
                  inputRouteStatus = InputRouteStatus.NoRayBanInput,
              ),
          onSurfaceChanged = {},
          onStartSession = {},
          onEndSession = {},
          onStartPreview = {},
          onStopPreview = {},
          onCaptureImage = {},
          onRetakeImage = {},
          onPromptChanged = {},
          onStartListening = {},
          onStopListening = {},
          onClearPrompt = {},
          onPrepareRecipeRequest = {},
          onSpeakRecipe = {},
          onStopRecipeSpeech = {},
      )
    }

    composeTestRule.onNodeWithTag("food_message_banner").assertIsDisplayed()
    composeTestRule
        .onNodeWithText(composeTestRule.activity.getString(R.string.food_open_bluetooth_settings))
        .assertIsDisplayed()
  }

  @Test
  fun stopTtsEnabledOnlyWhileSpeaking() {
    composeTestRule.setContent {
      FoodRecipeContent(
          uiState =
              FoodRecipeUiState(
                  recipeText = "Recipe result",
                  ttsPlaybackStatus = TtsPlaybackStatus.Playing,
                  recognitionStatus = RecognitionStatus.Completed,
                  audioRouteStatus = AudioRouteStatus.RayBanCandidateFound,
              ),
          onSurfaceChanged = {},
          onStartSession = {},
          onEndSession = {},
          onStartPreview = {},
          onStopPreview = {},
          onCaptureImage = {},
          onRetakeImage = {},
          onPromptChanged = {},
          onStartListening = {},
          onStopListening = {},
          onClearPrompt = {},
          onPrepareRecipeRequest = {},
          onSpeakRecipe = {},
          onStopRecipeSpeech = {},
      )
    }

    composeTestRule.onNodeWithTag("food_stop_tts_button").assertIsEnabled()
  }
}
