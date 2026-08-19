package com.mtvs.food.food.recipe

data class RecipeResponseDto(
    val recipeText: String,
    val model: String,
    val elapsedMs: Long,
)

data class RecipeErrorDto(
    val message: String,
    val code: String,
)

data class RecipeRequestPayload(
    val imageBytes: ByteArray,
    val imageMimeType: String,
    val imageFileName: String,
    val prompt: String,
    val language: String,
)
