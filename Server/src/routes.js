import { Router } from "express";
import { AppError } from "./errors.js";
import { recipeUpload } from "./middleware.js";
import { logError, logInfo } from "./logger.js";

function normalizeLanguage(language) {
  const normalized = String(language || "").trim();
  return normalized || "ko-KR";
}

function validateRecipeRequest(request) {
  if (!request.file) {
    throw new AppError(400, "IMAGE_REQUIRED", "image is required.");
  }

  const prompt = String(request.body?.prompt || "").trim();
  if (!prompt) {
    throw new AppError(400, "PROMPT_REQUIRED", "prompt is required.");
  }

  return {
    prompt,
    language: normalizeLanguage(request.body?.language),
    imageBuffer: request.file.buffer,
    imageMimeType: request.file.mimetype,
  };
}

export function createRouter({ config, recipeService }) {
  const router = Router();

  router.get("/health", (_request, response) => {
    response.json({
      status: "ok",
      port: config.serverPort,
      host: "0.0.0.0",
      model: config.nvidiaModel,
      baseUrl: config.nvidiaBaseUrl,
      hasApiKey: Boolean(config.nvidiaApiKey),
    });
  });

  router.post("/api/recipe", recipeUpload.single("image"), async (request, response, next) => {
    const requestId = `recipe-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

    try {
      const startedAt = Date.now();
      const { prompt, language, imageBuffer, imageMimeType } = validateRecipeRequest(request);

      logInfo("Recipe request accepted", {
        requestId,
        promptLength: prompt.length,
        promptPreview: prompt.slice(0, 80),
        language,
        imageMimeType,
        imageBytes: imageBuffer.length,
      });

      const recipe = await recipeService.createRecipe({
        requestId,
        imageBuffer,
        imageMimeType,
        prompt,
        language,
      });

      const elapsedMs = Date.now() - startedAt;
      logInfo("Recipe request completed", {
        requestId,
        elapsedMs,
        responseTextLength: recipe.recipeText.length,
        model: recipe.model || config.nvidiaModel,
      });

      response.json({
        recipeText: recipe.recipeText,
        model: recipe.model || config.nvidiaModel,
        elapsedMs,
      });
    } catch (error) {
      logError("Recipe request failed", {
        requestId,
        name: error?.name,
        message: error?.message,
        code: error?.code,
        status: error?.status,
      });
      next(error);
    }
  });

  return router;
}
