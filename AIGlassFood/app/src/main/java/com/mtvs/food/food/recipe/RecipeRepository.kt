package com.mtvs.food.food.recipe

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.mtvs.food.BuildConfig
import java.io.IOException

class RecipeRepository(
    private val contentResolver: ContentResolver,
    private val recipeApiClient: RecipeApiClient = RecipeApiClient(BuildConfig.FOOD_SERVER_BASE_URL),
) {
  suspend fun requestRecipe(imageUri: Uri, prompt: String, language: String = "ko-KR"): RecipeResponseDto {
    val normalizedPrompt = prompt.trim()
    if (normalizedPrompt.isBlank()) {
      throw RecipeApiException(
          kind = RecipeApiErrorKind.Validation,
          userMessage = "Speak or type what you want to cook first.",
      )
    }

    val imageBytes =
        contentResolver.openInputStream(imageUri)?.use { input -> input.readBytes() }
            ?: throw RecipeApiException(
                kind = RecipeApiErrorKind.Validation,
                userMessage = "Capture a food photo first.",
            )

    val mimeType =
        contentResolver.getType(imageUri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(imageUri.toString()).lowercase()
                )
            ?: "image/png"

    val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

    return recipeApiClient.requestRecipe(
        RecipeRequestPayload(
            imageBytes = imageBytes,
            imageMimeType = mimeType,
            imageFileName = "food_capture.$fileExtension",
            prompt = normalizedPrompt,
            language = language,
        )
    )
  }

  suspend fun checkHealth(): Result<Unit> = recipeApiClient.checkHealth()
}
