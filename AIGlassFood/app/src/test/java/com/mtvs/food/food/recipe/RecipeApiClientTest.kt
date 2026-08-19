package com.mtvs.food.food.recipe

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeApiClientTest {
  private lateinit var server: MockWebServer
  private lateinit var client: RecipeApiClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    client = RecipeApiClient(server.url("/").toString(), OkHttpClient())
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun requestRecipe_sendsMultipartBody_andParsesSuccess() {
    server.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"recipeText":"recipe body","model":"demo-model","elapsedMs":123}""")
    )

    val response =
        runBlockingRecipe {
          client.requestRecipe(
              RecipeRequestPayload(
                  imageBytes = "image".toByteArray(),
                  imageMimeType = "image/png",
                  imageFileName = "food.png",
                  prompt = "Quick dinner",
                  language = "ko-KR",
              )
          )
        }

    val recordedRequest = server.takeRequest()
    assertEquals("/api/recipe", recordedRequest.path)
    assertTrue(recordedRequest.getHeader("Content-Type").orEmpty().contains("multipart/form-data"))
    assertTrue(recordedRequest.body.readUtf8().contains("Quick dinner"))
    assertEquals("recipe body", response.recipeText)
    assertEquals("demo-model", response.model)
    assertEquals(123L, response.elapsedMs)
  }

  @Test
  fun requestRecipe_mapsValidationErrors() {
    server.enqueue(
        MockResponse()
            .setResponseCode(400)
            .setBody("""{"message":"prompt is required.","code":"PROMPT_REQUIRED"}""")
    )

    val error =
        runCatching {
              runBlockingRecipe {
                client.requestRecipe(
                    RecipeRequestPayload(
                        imageBytes = "image".toByteArray(),
                        imageMimeType = "image/png",
                        imageFileName = "food.png",
                        prompt = "",
                        language = "ko-KR",
                    )
                )
              }
            }
            .exceptionOrNull() as RecipeApiException

    assertEquals(RecipeApiErrorKind.Validation, error.kind)
    assertEquals("prompt is required.", error.userMessage)
  }

  @Test
  fun requestRecipe_mapsServerTimeout() {
    server.enqueue(
        MockResponse()
            .setResponseCode(504)
            .setBody("""{"message":"The recipe service took too long to respond.","code":"NVIDIA_TIMEOUT"}""")
    )

    val error =
        runCatching {
              runBlockingRecipe {
                client.requestRecipe(
                    RecipeRequestPayload(
                        imageBytes = "image".toByteArray(),
                        imageMimeType = "image/png",
                        imageFileName = "food.png",
                        prompt = "Quick dinner",
                        language = "ko-KR",
                    )
                )
              }
            }
            .exceptionOrNull() as RecipeApiException

    assertEquals(RecipeApiErrorKind.Timeout, error.kind)
    assertEquals("The recipe service took too long to respond.", error.userMessage)
  }

  @Test
  fun requestRecipe_mapsInvalidJson() {
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"recipeText":"""))

    val error =
        runCatching {
              runBlockingRecipe {
                client.requestRecipe(
                    RecipeRequestPayload(
                        imageBytes = "image".toByteArray(),
                        imageMimeType = "image/png",
                        imageFileName = "food.png",
                        prompt = "Quick dinner",
                        language = "ko-KR",
                    )
                )
              }
            }
            .exceptionOrNull() as RecipeApiException

    assertEquals(RecipeApiErrorKind.InvalidResponse, error.kind)
  }
}

private fun <T> runBlockingRecipe(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
