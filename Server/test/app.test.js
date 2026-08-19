import test from "node:test";
import assert from "node:assert/strict";
import request from "supertest";
import { AppError } from "../src/errors.js";
import { createApp } from "../src/app.js";

const config = {
  serverPort: 8000,
  nvidiaApiKey: "test-key",
  nvidiaBaseUrl: "https://integrate.api.nvidia.com/v1",
  nvidiaModel: "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
};

function buildApp({ recipeResult, recipeError } = {}) {
  const recipeService = {
    async createRecipe() {
      if (recipeError) {
        throw recipeError;
      }

      return (
          recipeResult || {
            recipeText:
                "1. Recipe Name\n2. Ingredients Seen In Image\n3. Additional Helpful Ingredients\n4. Estimated Cooking Time\n5. Cooking Steps\n6. Nutrition Notes\n7. Safety Notes",
            model: config.nvidiaModel,
          }
      );
    },
  };

  return createApp(config, { recipeService });
}

test("GET /health returns server status", async () => {
  const response = await request(buildApp()).get("/health");

  assert.equal(response.status, 200);
  assert.equal(response.body.status, "ok");
  assert.equal(response.body.port, config.serverPort);
});

test("POST /api/recipe accepts a valid multipart request", async () => {
  const response = await request(buildApp())
      .post("/api/recipe")
      .field("prompt", "Suggest a dinner I can make quickly.")
      .field("language", "ko-KR")
      .attach("image", Buffer.from("fake-image"), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 200);
  assert.equal(response.body.model, config.nvidiaModel);
  assert.match(response.body.recipeText, /Recipe Name/i);
});

test("POST /api/recipe rejects a missing image", async () => {
  const response = await request(buildApp())
      .post("/api/recipe")
      .field("prompt", "Healthy dinner");

  assert.equal(response.status, 400);
  assert.deepEqual(response.body, {
    message: "image is required.",
    code: "IMAGE_REQUIRED",
  });
});

test("POST /api/recipe rejects a blank prompt", async () => {
  const response = await request(buildApp())
      .post("/api/recipe")
      .field("prompt", "   ")
      .attach("image", Buffer.from("fake-image"), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 400);
  assert.deepEqual(response.body, {
    message: "prompt is required.",
    code: "PROMPT_REQUIRED",
  });
});

test("POST /api/recipe rejects an unsupported image type", async () => {
  const response = await request(buildApp())
      .post("/api/recipe")
      .field("prompt", "Healthy dinner")
      .attach("image", Buffer.from("plain-text"), {
        filename: "food.txt",
        contentType: "text/plain",
      });

  assert.equal(response.status, 400);
  assert.deepEqual(response.body, {
    message: "image must be a supported image file.",
    code: "INVALID_IMAGE_MIME_TYPE",
  });
});

test("POST /api/recipe rejects an image over 8 MB", async () => {
  const response = await request(buildApp())
      .post("/api/recipe")
      .field("prompt", "Healthy dinner")
      .attach("image", Buffer.alloc(8 * 1024 * 1024 + 1, 1), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 400);
  assert.deepEqual(response.body, {
    message: "image must be 8 MB or smaller.",
    code: "IMAGE_TOO_LARGE",
  });
});

test("POST /api/recipe maps NVIDIA timeout errors", async () => {
  const response = await request(
          buildApp({
            recipeError: new AppError(504, "NVIDIA_TIMEOUT", "The recipe service took too long to respond."),
          }),
      )
      .post("/api/recipe")
      .field("prompt", "Healthy dinner")
      .attach("image", Buffer.from("fake-image"), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 504);
  assert.deepEqual(response.body, {
    message: "The recipe service took too long to respond.",
    code: "NVIDIA_TIMEOUT",
  });
});

test("POST /api/recipe maps NVIDIA auth failures", async () => {
  const response = await request(
          buildApp({
            recipeError: new AppError(502, "NVIDIA_AUTH_FAILED", "The recipe service authentication failed."),
          }),
      )
      .post("/api/recipe")
      .field("prompt", "Healthy dinner")
      .attach("image", Buffer.from("fake-image"), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 502);
  assert.deepEqual(response.body, {
    message: "The recipe service authentication failed.",
    code: "NVIDIA_AUTH_FAILED",
  });
});

test("POST /api/recipe maps NVIDIA empty responses", async () => {
  const response = await request(
          buildApp({
            recipeError: new AppError(502, "NVIDIA_EMPTY_RESPONSE", "The recipe service returned an empty response."),
          }),
      )
      .post("/api/recipe")
      .field("prompt", "Healthy dinner")
      .attach("image", Buffer.from("fake-image"), {
        filename: "food.png",
        contentType: "image/png",
      });

  assert.equal(response.status, 502);
  assert.deepEqual(response.body, {
    message: "The recipe service returned an empty response.",
    code: "NVIDIA_EMPTY_RESPONSE",
  });
});
