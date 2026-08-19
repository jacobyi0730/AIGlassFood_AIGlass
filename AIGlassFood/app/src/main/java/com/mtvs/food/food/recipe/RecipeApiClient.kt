package com.mtvs.food.food.recipe

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

class RecipeApiClient(
    private val baseUrl: String,
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build(),
) {
  suspend fun requestRecipe(payload: RecipeRequestPayload): RecipeResponseDto {
    val requestUrl = baseUrl.ensureTrailingSlash() + "api/recipe"
    Log.d(
        TAG,
        "requestRecipe start url=$requestUrl promptLength=${payload.prompt.length} language=${payload.language} mimeType=${payload.imageMimeType} imageBytes=${payload.imageBytes.size}"
    )

    val request =
        Request.Builder()
            .url(requestUrl)
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name = "image",
                        filename = payload.imageFileName,
                        body = payload.imageBytes.toRequestBody(payload.imageMimeType.toMediaType()),
                    )
                    .addFormDataPart("prompt", payload.prompt)
                    .addFormDataPart("language", payload.language)
                    .build()
            )
            .build()

    val responseBody =
        withContext(Dispatchers.IO) {
          execute(request).use { response ->
            val rawBody = response.body?.string().orEmpty()

            Log.d(
                TAG,
                "requestRecipe response status=${response.code} isSuccessful=${response.isSuccessful} bodyLength=${rawBody.length}"
            )

            if (!response.isSuccessful) {
              throw mapError(response.code, rawBody)
            }

            try {
              val json = JSONObject(rawBody)
              RecipeResponseDto(
                  recipeText = json.getString("recipeText"),
                  model = json.optString("model", ""),
                  elapsedMs = json.optLong("elapsedMs", 0L),
              )
            } catch (error: JSONException) {
              throw RecipeApiException(
                  kind = RecipeApiErrorKind.InvalidResponse,
                  userMessage = "The server returned an unreadable response.",
                  cause = error,
              )
            }
          }
        }

    return responseBody
  }

  suspend fun checkHealth(): Result<Unit> {
    val request = Request.Builder().url(baseUrl.ensureTrailingSlash() + "health").get().build()
    return runCatching {
      execute(request).use { response ->
        if (!response.isSuccessful) {
          throw mapError(response.code, response.body?.string().orEmpty())
        }
      }
    }
  }

  private suspend fun execute(request: Request) =
      suspendCancellableCoroutine<okhttp3.Response> { continuation ->
        val call = okHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : okhttp3.Callback {
              override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                Log.e(
                    TAG,
                    "HTTP request failed url=${request.url} canceled=${call.isCanceled()} message=${e.message}",
                    e,
                )
                continuation.resumeWithException(
                    RecipeApiException(
                        kind =
                            if (call.isCanceled()) {
                              RecipeApiErrorKind.Cancelled
                            } else if (e is SocketTimeoutException) {
                              RecipeApiErrorKind.Timeout
                            } else {
                              RecipeApiErrorKind.Network
                            },
                        userMessage =
                            if (call.isCanceled()) {
                              "The request was cancelled."
                            } else if (e is SocketTimeoutException) {
                              "The recipe request timed out while waiting for the PC server response."
                            } else {
                              "The Android app could not reach the PC server. Check the server IP, port, and that both devices are on the same Wi-Fi network."
                            },
                        cause = e,
                    )
                )
              }

              override fun onResponse(call: Call, response: okhttp3.Response) {
                Log.d(TAG, "HTTP response received url=${request.url} status=${response.code}")
                continuation.resume(response)
              }
            }
        )
      }

  private fun mapError(statusCode: Int, rawBody: String): RecipeApiException {
    val serverMessage = parseServerError(rawBody)

    return when (statusCode) {
      400 ->
          RecipeApiException(
              kind = RecipeApiErrorKind.Validation,
              userMessage = serverMessage?.message ?: "The recipe request is missing required fields.",
              serverCode = serverMessage?.code,
          )
      502, 503 ->
          RecipeApiException(
              kind = RecipeApiErrorKind.Server,
              userMessage =
                  when (serverMessage?.code) {
                    "NVIDIA_AUTH_FAILED" -> "The PC server could not authenticate with NVIDIA."
                    "NVIDIA_RATE_LIMITED" -> "NVIDIA is temporarily busy. Please try again shortly."
                    "NVIDIA_EMPTY_RESPONSE" -> "NVIDIA returned an empty recipe response."
                    else -> serverMessage?.message ?: "The PC server could not generate a recipe."
                  },
              serverCode = serverMessage?.code,
          )
      504 ->
          RecipeApiException(
              kind = RecipeApiErrorKind.Timeout,
              userMessage =
                  serverMessage?.message
                      ?: "The recipe request timed out. Check the PC server and try again.",
              serverCode = serverMessage?.code,
          )
      else ->
          RecipeApiException(
              kind = RecipeApiErrorKind.Server,
              userMessage =
                  serverMessage?.message ?: "The PC server returned an unexpected error.",
              serverCode = serverMessage?.code,
          )
    }
  }

  private fun parseServerError(rawBody: String): RecipeErrorDto? {
    return try {
      val json = JSONObject(rawBody)
      RecipeErrorDto(
          message = json.optString("message", ""),
          code = json.optString("code", ""),
      )
    } catch (_: JSONException) {
      null
    }
  }
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

private const val TAG = "FoodRecipeApi"

enum class RecipeApiErrorKind {
  Validation,
  Network,
  Timeout,
  Server,
  InvalidResponse,
  Cancelled,
}

class RecipeApiException(
    val kind: RecipeApiErrorKind,
    val userMessage: String,
    val serverCode: String? = null,
    cause: Throwable? = null,
) : IOException(userMessage, cause)
