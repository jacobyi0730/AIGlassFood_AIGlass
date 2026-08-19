import { Router } from "express";
import { AppError } from "./errors.js";
import { recipeUpload } from "./middleware.js";

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
    try {
      const startedAt = Date.now();
      const { prompt, language, imageBuffer, imageMimeType } = validateRecipeRequest(request);

      const recipe = await recipeService.createRecipe({
        imageBuffer,
        imageMimeType,
        prompt,
        language,
      });

      response.json({
        recipeText: recipe.recipeText,
        model: recipe.model || config.nvidiaModel,
        elapsedMs: Date.now() - startedAt,
      });
    } catch (error) {
      next(error);
    }
  });

  return router;
}
